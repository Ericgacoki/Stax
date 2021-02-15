package com.hover.stax.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.hover.stax.channels.Channel;
import com.hover.stax.channels.ChannelDao;
import com.hover.stax.contacts.ContactDao;
import com.hover.stax.contacts.StaxContact;
import com.hover.stax.requests.Request;
import com.hover.stax.requests.RequestDao;
import com.hover.stax.schedules.Schedule;
import com.hover.stax.schedules.ScheduleDao;
import com.hover.stax.transactions.StaxTransaction;
import com.hover.stax.transactions.TransactionDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Channel.class, StaxTransaction.class, StaxContact.class, Request.class, Schedule.class}, version = 25)
public abstract class AppDatabase extends RoomDatabase {
	private static final int NUMBER_OF_THREADS = 8;
	static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

	private static volatile AppDatabase INSTANCE;

	public abstract ChannelDao channelDao();

	public abstract TransactionDao transactionDao();

	public abstract ContactDao contactDao();

	public abstract RequestDao requestDao();

	public abstract ScheduleDao scheduleDao();

	public static synchronized AppDatabase getInstance(Context context) {
		if (INSTANCE == null) {
			synchronized (AppDatabase.class) {
				if (INSTANCE == null) {
					INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "stax.db")
						.setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
						.addMigrations(M23_24)
						.addMigrations(M24_25)
						.build();
				}
			}
		}
		return INSTANCE;
	}

	static final Migration M23_24 = new Migration(23, 24) {
		@Override
		public void migrate(SupportSQLiteDatabase database) {
		}
	};

	static final Migration M24_25 = new Migration(24, 25) {
		@Override
		public void migrate(SupportSQLiteDatabase database) {
			database.execSQL("DROP INDEX index_stax_contacts_lookup_key");
			database.execSQL("DROP INDEX index_stax_contacts_id_phone_number");
			database.execSQL("CREATE UNIQUE INDEX index_stax_contacts_id ON stax_contacts(id)");
			database.execSQL("CREATE UNIQUE INDEX index_stax_contacts_phone_number ON stax_contacts(phone_number)");
		}
	};
}
