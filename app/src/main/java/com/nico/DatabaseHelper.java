package com.nico;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    // Информация о базе данных
    private static final String DATABASE_NAME = "NicoMessenger.db";
    private static final int DATABASE_VERSION = 1;
    
    // Названия таблиц и колонок
    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CHAT_NAME = "chat_name";
    private static final String COLUMN_SENDER = "sender";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_IS_OUTGOING = "is_outgoing";
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создаем таблицу сообщений
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CHAT_NAME + " TEXT,"
                + COLUMN_SENDER + " TEXT,"
                + COLUMN_MESSAGE + " TEXT,"
                + COLUMN_TIMESTAMP + " TEXT,"
                + COLUMN_IS_OUTGOING + " INTEGER" + ")";
        
        db.execSQL(CREATE_MESSAGES_TABLE);
        
        System.out.println("🗃️ Nico: Database created successfully!");
        
        // Добавляем тестовые сообщения
        addSampleMessages(db);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // При обновлении базы удаляем старую таблицу
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }
    
    // Добавляем тестовые сообщения
    private void addSampleMessages(SQLiteDatabase db) {
        addMessage(db, "Alex", "Alex", "Hey! How's Nico working?", "10:30 AM", 0);
        addMessage(db, "Alex", "You", "It's amazing! Love the iOS design", "10:31 AM", 1);
        addMessage(db, "Alex", "Alex", "The Liquid Glass effects are so smooth! 💙", "10:32 AM", 0);
        
        addMessage(db, "Sarah", "Sarah", "Love the iOS design! 💙", "9:15 AM", 0);
        addMessage(db, "Sarah", "You", "Thanks! Working hard on Nico", "9:16 AM", 1);
        
        System.out.println("💾 Nico: Sample messages added to database");
    }
    
    // Метод для добавления сообщения
    public long addMessage(String chatName, String sender, String message, String timestamp, boolean isOutgoing) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CHAT_NAME, chatName);
        values.put(COLUMN_SENDER, sender);
        values.put(COLUMN_MESSAGE, message);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_IS_OUTGOING, isOutgoing ? 1 : 0);
        
        long result = db.insert(TABLE_MESSAGES, null, values);
        db.close();
        
        System.out.println("💾 Nico: Message saved to database - " + message);
        return result;
    }
    
    // Внутренний метод для добавления тестовых сообщений
    private void addMessage(SQLiteDatabase db, String chatName, String sender, String message, String timestamp, int isOutgoing) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CHAT_NAME, chatName);
        values.put(COLUMN_SENDER, sender);
        values.put(COLUMN_MESSAGE, message);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_IS_OUTGOING, isOutgoing);
        
        db.insert(TABLE_MESSAGES, null, values);
    }
    
    // Получаем все сообщения для конкретного чата
    public List<Message> getMessagesForChat(String chatName) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_MESSAGES,
                new String[]{COLUMN_ID, COLUMN_SENDER, COLUMN_MESSAGE, COLUMN_TIMESTAMP, COLUMN_IS_OUTGOING},
                COLUMN_CHAT_NAME + " = ?",
                new String[]{chatName},
                null, null, COLUMN_ID + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                Message message = new Message(
                    cursor.getString(1), // sender
                    cursor.getString(2), // message
                    cursor.getString(3), // timestamp
                    cursor.getInt(4) == 1 // isOutgoing
                );
                messages.add(message);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        System.out.println("📨 Nico: Loaded " + messages.size() + " messages for chat: " + chatName);
        return messages;
    }
    
    // Получаем последние сообщения для всех чатов (для главного экрана)
    public List<Chat> getRecentChats() {
        List<Chat> chats = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // SQL запрос для получения последнего сообщения из каждого чата
        String query = "SELECT " + COLUMN_CHAT_NAME + ", " + COLUMN_MESSAGE + ", " + COLUMN_TIMESTAMP +
                      " FROM " + TABLE_MESSAGES +
                      " WHERE " + COLUMN_ID + " IN (" +
                      "SELECT MAX(" + COLUMN_ID + ") FROM " + TABLE_MESSAGES +
                      " GROUP BY " + COLUMN_CHAT_NAME + ")";
        
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                Chat chat = new Chat(
                    cursor.getString(0), // chat_name
                    cursor.getString(1), // last message
                    cursor.getString(2)  // timestamp
                );
                chats.add(chat);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        System.out.println("💬 Nico: Loaded " + chats.size() + " recent chats");
        return chats;
    }
    
    // Модель сообщения для базы данных
    public static class Message {
        public String sender;
        public String text;
        public String time;
        public boolean isOutgoing;
        
        public Message(String sender, String text, String time, boolean isOutgoing) {
            this.sender = sender;
            this.text = text;
            this.time = time;
            this.isOutgoing = isOutgoing;
        }
    }
}
