package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.ManagerUserEntity
import com.tencent.devops.model.auth.tables.TAuthManagerUser
import com.tencent.devops.model.auth.tables.records.TAuthManagerUserRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

@Repository
class ManagerUserDao {

    fun create(dslContext: DSLContext, mangerUserInfo: ManagerUserEntity): Int {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.insertInto(this,
                USER_ID,
                START_TIME,
                END_TIME,
                CREATE_TIME,
                CREATE_USER,
                UPDATE_TIME,
                UPDATE_USER
            ).values(
                mangerUserInfo.userId,
                Timestamp(mangerUserInfo.startTime).toLocalDateTime(),
                Timestamp(mangerUserInfo.timeoutTime).toLocalDateTime(),
                LocalDateTime.now(),
                mangerUserInfo.createUser,
                null,
                ""
            ).execute()
        }
    }

    fun list(dslContext: DSLContext, mangerId: Int): Result<TAuthManagerUserRecord>? {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectFrom(this).where(MANGER_ID.eq(mangerId).and(END_TIME.gt(LocalDateTime.now()))).orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    fun count(dslContext: DSLContext, mangerId: Int): Int? {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectFrom(this).where(MANGER_ID.eq(mangerId).and(END_TIME.gt(LocalDateTime.now()))).count()
        }
    }
}
