/*
 * Copyright 1999-2021 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.plugin.auth.impl.persistence;

import com.alibaba.nacos.config.server.configuration.ConditionOnExternalStorage;
import com.alibaba.nacos.config.server.model.Page;
import com.alibaba.nacos.config.server.service.repository.PaginationHelper;
import com.alibaba.nacos.config.server.service.repository.extrnal.ExternalStoragePersistServiceImpl;
import com.alibaba.nacos.config.server.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static com.alibaba.nacos.plugin.auth.impl.persistence.AuthRowMapperManager.USER_ROW_MAPPER;

/**
 * Implemetation of ExternalUserPersistServiceImpl.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
@Conditional(value = ConditionOnExternalStorage.class)
@Component
public class ExternalUserPersistServiceImpl implements UserPersistService {
    
    @Autowired
    private ExternalStoragePersistServiceImpl persistService;
    
    private JdbcTemplate jt;
    
    @PostConstruct
    protected void init() {
        jt = persistService.getJdbcTemplate();
    }
    
    /**
     * Execute create user operation.
     *
     * @param username username string value.
     * @param password password string value.
     */
    @Override
    public void createUser(String username, String password) {
        String sql = "INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)";
        
        try {
            jt.update(sql, username, password, true);
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e.toString(), e);
            throw e;
        }
    }
    
    /**
     * Execute delete user operation.
     *
     * @param username username string value.
     */
    @Override
    public void deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username=?";
        try {
            jt.update(sql, username);
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e.toString(), e);
            throw e;
        }
    }
    
    /**
     * Execute update user password operation.
     *
     * @param username username string value.
     * @param password password string value.
     */
    @Override
    public void updateUserPassword(String username, String password) {
        try {
            jt.update("UPDATE users SET password = ? WHERE username=?", password, username);
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e.toString(), e);
            throw e;
        }
    }
    
    /**
     * Execute find user by username operation.
     *
     * @param username username string value.
     * @return User model.
     */
    @Override
    public User findUserByUsername(String username) {
        String sql = "SELECT username,password FROM users WHERE username=? ";
        try {
            return this.jt.queryForObject(sql, new Object[] {username}, USER_ROW_MAPPER);
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e.toString(), e);
            throw e;
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            LogUtil.FATAL_LOG.error("[db-other-error]" + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Page<User> getUsers(int pageNo, int pageSize) {
        
        PaginationHelper<User> helper = persistService.createPaginationHelper();
        
        String sqlCountRows = "SELECT count(*) FROM users WHERE ";
        
        String sqlFetchRows = "SELECT username,password FROM users WHERE ";
        
        String where = " 1=1 ";
        
        try {
            Page<User> pageInfo = helper
                    .fetchPage(sqlCountRows + where, sqlFetchRows + where, new ArrayList<String>().toArray(), pageNo,
                            pageSize, USER_ROW_MAPPER);
            if (pageInfo == null) {
                pageInfo = new Page<>();
                pageInfo.setTotalCount(0);
                pageInfo.setPageItems(new ArrayList<>());
            }
            return pageInfo;
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e.toString(), e);
            throw e;
        }
    }
    
    @Override
    public List<String> findUserLikeUsername(String username) {
        String sql = "SELECT username FROM users WHERE username LIKE '%" + username + "%'";
        List<String> users = this.jt.queryForList(sql, null, String.class);
        return users;
    }
}
