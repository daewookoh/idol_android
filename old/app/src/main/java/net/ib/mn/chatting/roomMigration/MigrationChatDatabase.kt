package net.ib.mn.chatting.roomMigration

import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.ib.mn.utils.Logger


/**
 db  마이그레이션 관련 설명

 채팅용  로컬 db 를  수정 하여,   version을  올렸을때는  아래와 같이  migration 코드를 작성합니다!
 버전은  start version 과  end version을 사용하여,  start -> end 로 이전을 진행합니다.
 아래 형식으로   각각의   db 이전에 대한  로직을  구성합니다.
 val MIGRATE_(start_version)_(end_version) = object : Migration(start_version, end_version) {

 구성이 다되었다면, chatDb클래스   databaseBuilder ->  addMigrations에  해당  migration 코드를 적용합니다
 **/

object  MigrationChatDatabase {

// 버전별 변경내역 생성 : 버전 1에서 2로 바뀔 때 적용되는 사항
//    val MIGRATE_1_2 = object : Migration(1, 2) {
//        override fun migrate(database: SupportSQLiteDatabase) {
//            /* 변경 쿼리 */
//            val alter = "ALTER table room_memo add column new_title text"
//            // 쿼리 적용
//            database.execSQL(alter)
//        }
//    }

    val MIGRATE_24_25 = object : Migration(24, 25) {
        override fun migrate(database: SupportSQLiteDatabase) {
        }
    }

    val MIGRATE_28_29 = object : Migration(28, 29) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE chat_message set reported =:reported WHERE user_id=:userId AND server_ts=:serverTs AND room_id=:roomId")
        }
    }

    // TODO: 2021/05/07  챗 멤버 모델에  most 아이디 용 컬럼 추가  -> 29에서  30로  올라감.
    val MIGRATE_29_30 = object : Migration(29, 30) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE chat_members ADD COLUMN most INTEGER NOT NULL DEFAULT -1")
        }
    }

    // TODO: 2021/05/08  멥버  most 컬럼    most_id 로 수정
    /// rename to  가  sql 버전  3.25 에서  안먹히는 이슈가 있어서  그냥  아래처럼  진행함.
    val MIGRATE_30_31 = object : Migration(30, 31) {
        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL("CREATE TABLE new_chat_members (id INTEGER NOT NULL, image_url TEXT,level INTEGER NOT NULL,nickname TEXT NOT NULL,role TEXT NOT NULL,room_id INTEGER,most_id INTEGER NOT NULL,PRIMARY KEY(id))")
            database.execSQL("INSERT INTO new_chat_members (id, image_url, level,nickname, role, room_id, most_id) SELECT id, image_url, level,nickname,role ,room_id, most FROM chat_members")
            database.execSQL("DROP TABLE chat_members");
            database.execSQL("ALTER TABLE new_chat_members RENAME TO chat_members");

        }
    }

    // TODO: 2021/05/10 chat_message 테이블에 isFirstJoinMsg 컬럼을 추가함. ->  맨처음 들어왔을때  나올 참여함을 알리는 메세지
    val MIGRATE_31_32 = object : Migration(31, 32) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE chat_message ADD COLUMN is_first_join_msg INTEGER NOT NULL DEFAULT 0")
          }
    }

    val MIGRATE_32_33 = object : Migration(32, 33) {
        override fun migrate(database: SupportSQLiteDatabase) {
        }
    }

    val MIGRATE_33_34 = object : Migration(33, 34) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE new_chat_members (id INTEGER NOT NULL, image_url TEXT,level INTEGER NOT NULL,nickname TEXT NOT NULL,role TEXT NOT NULL,room_id INTEGER,most_id INTEGER NOT NULL,deleted INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(id))")
            database.execSQL("INSERT INTO new_chat_members (id, image_url, level,nickname, role, room_id, most_id) SELECT id, image_url, level,nickname,role ,room_id, most_id FROM chat_members")
            database.execSQL("DROP TABLE chat_members")
            database.execSQL("ALTER TABLE new_chat_members RENAME TO chat_members");
        }
    }

    // TODO: 2021/05/17  채팅 메세지 ->  is_link_url 을   추가 하고  default 값을 0으로 넣어줌.
    val MIGRATE_34_35 = object : Migration(34, 35) {
        override fun migrate(database: SupportSQLiteDatabase) {
            //메시지 테이블 링크추가.
            database.execSQL("ALTER TABLE chat_message ADD COLUMN is_link_url INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATE_35_36 = object : Migration(35, 36) {
        override fun migrate(database: SupportSQLiteDatabase) {
            //멤버테이블 room_id PRIMARY KEY추가
            database.execSQL("CREATE TABLE new_chat_members (id INTEGER NOT NULL, image_url TEXT,level INTEGER NOT NULL,nickname TEXT NOT NULL,role TEXT NOT NULL,room_id INTEGER NOT NULL,most_id INTEGER NOT NULL,deleted INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(id, room_id))")
            database.execSQL("INSERT INTO new_chat_members (id, image_url, level,nickname, role, room_id, most_id) SELECT id, image_url, level,nickname,role ,room_id, most_id FROM chat_members")
            database.execSQL("DROP TABLE chat_members")
            database.execSQL("ALTER TABLE new_chat_members RENAME TO chat_members");
        }
    }

    val MIGRATE_36_37 = object : Migration(36, 37) {
        override fun migrate(database: SupportSQLiteDatabase) {
            //채팅방 정보 추가.
            database.execSQL("CREATE TABLE chat_room_info (created_at INTEGER , cur_people INTEGER , description TEXT , gcode INTEGER , idol_id INTEGER , " +
                "is_anonymity TEXT , is_default TEXT , is_most_only TEXT , last_msg TEXT , last_msg_time INTEGER , level_limit INTEGER , locale TEXT , " +
                "max_people INTEGER , id INTEGER NOT NULL, socket_url TEXT , success INTEGER , title TEXT , total_msg_cnt INTEGER , user_id INTEGER ,PRIMARY KEY(id))")
        }
    }
}
