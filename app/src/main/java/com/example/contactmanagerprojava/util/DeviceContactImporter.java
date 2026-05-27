package com.example.contactmanagerprojava.util;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.ContactEntity;
import com.example.contactmanagerprojava.data.ContactPhoneEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tiện ích đọc danh bạ thật từ thiết bị qua Android Contacts Provider và nhập vào Room Database.
 */
public class DeviceContactImporter {
    private DeviceContactImporter() {}

    /**
     * Đọc danh bạ từ Android Contacts Provider, chuẩn hóa dữ liệu và lưu các liên hệ chưa bị trùng vào Room Database.
     */
    public static ImportResult importContacts(Context context, AppRepository repository, long userId) {
        ImportResult result = new ImportResult();
        if (context == null || repository == null || userId <= 0L) {
            result.failed = true;
            result.message = "Không thể xác định tài khoản người dùng trong app.";
            return result;
        }

        Set<String> existingPhones = loadExistingNormalizedPhones(repository, userId);
        Map<String, DeviceContact> deviceContacts = readDeviceContacts(context, result);
        readDeviceEmails(context, deviceContacts);

        for (DeviceContact deviceContact : deviceContacts.values()) {
            if (deviceContact.phones.isEmpty()) {
                result.skippedNoPhone++;
                continue;
            }

            boolean duplicate = false;
            for (DevicePhone phone : deviceContact.phones) {
    /**
     * Chuẩn hóa số điện thoại về dạng chỉ còn chữ số để so sánh trùng lặp.
     */
                String normalized = normalizePhone(phone.number);
                if (!normalized.isEmpty() && existingPhones.contains(normalized)) {
                    duplicate = true;
                    break;
                }
            }

            if (duplicate) {
                result.skippedDuplicate++;
                continue;
            }

            ContactEntity contact = new ContactEntity(userId, safeName(deviceContact.name));
            contact.email = emptyToNull(deviceContact.email);
            contact.tags = null;
            contact.note = null;
            contact.updatedAt = System.currentTimeMillis();

            List<ContactPhoneEntity> phones = new ArrayList<>();
            boolean first = true;
            for (DevicePhone phone : deviceContact.phones) {
                String number = cleanPhoneDisplay(phone.number);
    /**
     * Chuẩn hóa số điện thoại về dạng chỉ còn chữ số để so sánh trùng lặp.
     */
                String normalized = normalizePhone(number);
                if (normalized.isEmpty()) continue;
                phones.add(new ContactPhoneEntity(0L, number, phone.label, first));
                existingPhones.add(normalized);
                first = false;
            }

            if (phones.isEmpty()) {
                result.skippedNoPhone++;
                continue;
            }

    /**
     * Lưu thông tin liên hệ và danh sách số điện thoại tương ứng vào Room Database.
     */
            repository.saveContact(contact, phones);
            result.imported++;
        }

        result.totalRead = deviceContacts.size();
        return result;
    }

    // Chuẩn hóa số điện thoại về dạng chỉ còn chữ số để so sánh chống trùng.
    /**
     * Chuẩn hóa số điện thoại về dạng chỉ còn chữ số để so sánh trùng lặp.
     */
    public static String normalizePhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("84") && digits.length() >= 10) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    // Tải toàn bộ số điện thoại đã có của user và chuẩn hóa để kiểm tra trùng.
    private static Set<String> loadExistingNormalizedPhones(AppRepository repository, long userId) {
        Set<String> out = new HashSet<>();
        List<ContactPhoneEntity> phones = repository.getPhonesForUser(userId);
        if (phones == null) return out;
        for (ContactPhoneEntity phone : phones) {
    /**
     * Chuẩn hóa số điện thoại về dạng chỉ còn chữ số để so sánh trùng lặp.
     */
            String normalized = normalizePhone(phone.phoneNumber);
            if (!normalized.isEmpty()) out.add(normalized);
        }
        return out;
    }

    // Đọc tên và số điện thoại từ Android Contacts Provider.
    private static Map<String, DeviceContact> readDeviceContacts(Context context, ImportResult result) {
        Map<String, DeviceContact> contacts = new LinkedHashMap<>();
        String[] projection = new String[]{
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Phone.TYPE,
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Phone.LABEL
        };

        // API ngoài - ContentResolver: truy vấn dữ liệu từ provider hệ thống như danh bạ Android.
        Cursor cursor = context.getContentResolver().query(
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC"
        );

        if (cursor == null) return contacts;

        try {
            Resources resources = context.getResources();
            // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
            int idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
            int nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY);
            // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
            int numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
            // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
            int typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE);
            // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
            int labelIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL);

            while (cursor.moveToNext()) {
                String contactId = cursor.getString(idIndex);
                String name = cursor.getString(nameIndex);
                String number = cursor.getString(numberIndex);
                int type = cursor.getInt(typeIndex);
                String customLabel = cursor.getString(labelIndex);

    /**
     * Chuẩn hóa số điện thoại về dạng chỉ còn chữ số để so sánh trùng lặp.
     */
                String normalized = normalizePhone(number);
                if (normalized.isEmpty()) {
                    result.skippedNoPhone++;
                    continue;
                }

                String key = contactId == null || contactId.trim().isEmpty()
                        ? safeName(name).toLowerCase(Locale.ROOT) + "-" + normalized
                        : contactId;
                DeviceContact contact = contacts.get(key);
                if (contact == null) {
                    contact = new DeviceContact(contactId, safeName(name));
                    contacts.put(key, contact);
                }

                if (contact.normalizedPhones.add(normalized)) {
                    // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                    String label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(resources, type, customLabel).toString();
                    contact.phones.add(new DevicePhone(number, label));
                }
            }
        } finally {
            cursor.close();
        }
        return contacts;
    }

    // Đọc email từ Android Contacts Provider và ghép vào contact đã đọc.
    private static void readDeviceEmails(Context context, Map<String, DeviceContact> contacts) {
        if (contacts.isEmpty()) return;

        String[] projection = new String[]{
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Email.ADDRESS
        };

        // API ngoài - ContentResolver: truy vấn dữ liệu từ provider hệ thống như danh bạ Android.
        Cursor cursor = context.getContentResolver().query(
                // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection,
                null,
                null,
                null
        );

        if (cursor == null) return;

        try {
            // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
            int idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID);
            // API ngoài - Android Contacts Provider: truy vấn danh bạ hệ thống trên thiết bị.
            int emailIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS);
            while (cursor.moveToNext()) {
                String contactId = cursor.getString(idIndex);
                String email = cursor.getString(emailIndex);
                if (contactId == null || email == null || email.trim().isEmpty()) continue;
                DeviceContact contact = contacts.get(contactId);
                if (contact != null && (contact.email == null || contact.email.trim().isEmpty())) {
                    contact.email = email.trim();
                }
            }
        } finally {
            cursor.close();
        }
    }

    // Chuẩn hóa giá trị đầu vào để tránh null hoặc chuỗi rỗng không mong muốn.
    private static String safeName(String name) {
        if (name == null || name.trim().isEmpty()) return "Không tên";
        return name.trim();
    }

    // Làm sạch số điện thoại để hiển thị/lưu giữ ở dạng dễ đọc.
    private static String cleanPhoneDisplay(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s+", " ");
    }

    // Chuẩn hóa giá trị đầu vào để tránh null hoặc chuỗi rỗng không mong muốn.
    private static String emptyToNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        return raw.trim();
    }

    public static class ImportResult {
        public int totalRead;
        public int imported;
        public int skippedDuplicate;
        public int skippedNoPhone;
        public boolean failed;
        public String message;

        public String toUserMessage() {
            if (failed) {
                return message == null ? "Nhập danh bạ thất bại." : message;
            }
            return "Đã nhập " + imported + " liên hệ. Bỏ qua " + skippedDuplicate + " liên hệ trùng.";
        }
    }

    private static class DeviceContact {
        final String contactId;
        final String name;
        String email;
        final List<DevicePhone> phones = new ArrayList<>();
        final Set<String> normalizedPhones = new HashSet<>();

        DeviceContact(String contactId, String name) {
            this.contactId = contactId;
            this.name = name;
        }
    }

    private static class DevicePhone {
        final String number;
        final String label;

        DevicePhone(String number, String label) {
            this.number = number;
            this.label = label == null || label.trim().isEmpty() ? "Di động" : label.trim();
        }
    }
}
