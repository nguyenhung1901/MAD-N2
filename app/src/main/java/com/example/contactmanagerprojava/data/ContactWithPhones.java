package com.example.contactmanagerprojava.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * Model quan hệ Room dùng để lấy một ContactEntity kèm danh sách ContactPhoneEntity tương ứng.
 */
public class ContactWithPhones {
    @Embedded public ContactEntity contact;
    @Relation(parentColumn = "id", entityColumn = "contactId")
    public List<ContactPhoneEntity> phones;
}
