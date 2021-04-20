package com.hover.stax.contacts;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.hover.sdk.actions.HoverAction;
import com.hover.stax.R;
import com.hover.stax.channels.Channel;
import com.hover.stax.utils.DateUtils;
import com.hover.stax.utils.Utils;

import java.util.List;
import java.util.UUID;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH;

@Entity(tableName = "stax_contacts", indices = {@Index(value ="id", unique = true), @Index(value ="phone_number", unique = true)} )
public class StaxContact {
	private final static String TAG = "StaxContact";
	public final static String ID_KEY = "contact_id";

	@PrimaryKey
	@NonNull
	public String id;

	@ColumnInfo(name = "lookup_key")
	public String lookupKey;

	@ColumnInfo(name = "name")
	public String name;

	@ColumnInfo(name = "aliases")
	public String aliases;

	@ColumnInfo(name = "phone_number")
	public String phoneNumber;

	@ColumnInfo(name = "thumb_uri")
	public String thumbUri;

	@ColumnInfo(name = "last_used_timestamp")
	public Long lastUsedTimestamp;

	public StaxContact() {}

	public StaxContact(String phone) {
		id = UUID.randomUUID().toString();
		lastUsedTimestamp = DateUtils.now();
		phoneNumber = phone.replaceAll(" ", "");
		name = "";
	}

	public StaxContact(Intent data, Context c) {
		Uri contactData = data.getData();
		if (contactData != null) {
			Cursor cur = c.getContentResolver().query(contactData, null, null, null, null);
			if (cur != null && cur.getCount() > 0 && cur.moveToNext()) {
				id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
				Log.e("StaxContact", "pulled contact with id: " + id);
				lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
				name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				thumbUri = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));

				if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
					Cursor phones = c.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);
					if (phones != null && phones.moveToNext())
						phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll(" ", "");
					if (phones != null) phones.close();
				}
			}
			if (cur != null) cur.close();
		}
	}

	public String getNumberFormatForInput(HoverAction a, Channel c) {
		try {
			String format = a.getFormatInfo(HoverAction.PHONE_KEY);
			if (format != null && format.startsWith(String.valueOf(getPhone(c.countryAlpha2).getCountryCode())))
				return getInternationalNumberNoPlus(c.countryAlpha2);
			else
				return normalizeNumberByCountry(c.countryAlpha2);
		} catch (NumberParseException e) { Log.e(TAG, "Google phone number util failed.", e); }
		return phoneNumber;
	}

	public String normalizeNumberByCountry(String country) { return normalizeNumberByCountry(phoneNumber, country); }

	public static String normalizeNumberByCountry(String number, String country) {
		String phoneNumber = number;
		try {
			phoneNumber = convertToCountry(number, country);
			Log.e("Contact", "Normalized number: " + phoneNumber);
		} catch (NumberParseException e) {
			Log.e("Contact", "error formating number", e);
		}
		return phoneNumber;
	}

	private static String convertToCountry(String number, String country) throws NumberParseException, IllegalStateException {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try {
			Phonenumber.PhoneNumber phone = phoneUtil.parse(number, country);
			number = phoneUtil.formatNumberForMobileDialing(phone, country, false);
		} catch (IllegalStateException e) { Log.e(TAG, "Google phone number util failed.", e); }
		return number;
	}

	public String getInternationalNumber(String country) throws NumberParseException, IllegalStateException {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		Phonenumber.PhoneNumber phone = getPhone(country);
		phone.getCountryCode();
		String str = phoneUtil.format(phone, PhoneNumberUtil.PhoneNumberFormat.E164);
		return str;
	}


	public String getInternationalNumberNoPlus(String country) {
		try {
			return getInternationalNumber(country).replace("+", "");
		} catch (NumberParseException | IllegalStateException e) {
			Utils.logErrorAndReportToFirebase(TAG, "Failed to transform number for contact; doing it the old fashioned way.", e);
			return phoneNumber.replace("+", "");
		}
	}

	public static String getNationalNumber(String number, String country) {
		try {
			PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
			Phonenumber.PhoneNumber phone = phoneUtil.parse(number, country);
			return phoneUtil.getNationalSignificantNumber(phone);
		} catch (NumberParseException | IllegalStateException e) {
			Utils.logErrorAndReportToFirebase(TAG, "Failed to transform number for contact; doing it the old fashioned way.", e);
			return number.startsWith("+") ? number.substring(4) : number;
		}
	}

	private Phonenumber.PhoneNumber getPhone(String country) throws NumberParseException, IllegalStateException {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		return phoneUtil.parse(phoneNumber, country);
	}

	public String shortName() {
		return hasName() ? name : getPhoneNumber();
	}

	public void setPhoneNumber(String number) { phoneNumber = number; }
	public String getPhoneNumber() { return phoneNumber; }

	public boolean hasName() {
		return name != null && !name.isEmpty();
	}

	public static String shortName(List<StaxContact> contacts, Context c) {
		if (contacts == null || contacts.size() == 0) return null;
		else if (contacts.size() == 1) return contacts.get(0).shortName();
		else return c.getString(R.string.descrip_multcontacts, contacts.size());
	}

	private String obfusicatePhone() {
		if (phoneNumber.length() <= 3)
			return phoneNumber;
		else if (phoneNumber.length() <= 6)
			return phoneNumber.substring(0, 1) + repeat("*", phoneNumber.length() - 2) + phoneNumber.substring(phoneNumber.length() - 1);
		else if (phoneNumber.length() <= 8)
			return phoneNumber.substring(0, 2) + repeat("*", phoneNumber.length() - 4) + phoneNumber.substring(phoneNumber.length() - 2);
		else
			return repeat("*", phoneNumber.length() - 4) + phoneNumber.substring(phoneNumber.length() - 4);
	}

	String repeat(String s, int length) {
		return s.length() >= length ? s.substring(0, length) : repeat(s + s, length);
	}

	@NonNull
	@Override
	public String toString() {
		return (name + " " + phoneNumber).trim();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (!(other instanceof StaxContact)) return false;
		StaxContact otherContact = (StaxContact) other;
		return id.equals(otherContact.id) || phoneNumber.equals(otherContact.phoneNumber) || isSamePhone(otherContact);
	}

	private boolean isSamePhone(StaxContact other) {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		return phoneUtil.isNumberMatch(phoneNumber, other.phoneNumber) != NO_MATCH;
	}
}
