/** 
 * Copyright 2018-2028 Akaxin Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.akaxin.site.storage.sqlite;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akaxin.common.logs.LogUtils;
import com.akaxin.site.storage.bean.ApplyUserBean;
import com.akaxin.site.storage.sqlite.manager.SQLiteJDBCManager;
import com.akaxin.site.storage.sqlite.sql.SQLConst;

/**
 * 好友申请相关数据库操作
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2017-11-19 20:00:19
 */
public class SQLiteFriendApplyDao {
	private static final Logger logger = LoggerFactory.getLogger(SQLiteFriendApplyDao.class);
	private static final String FRIEND_APPLY_TABLE = SQLConst.SITE_FRIEND_APPLY;

	private static SQLiteFriendApplyDao instance = new SQLiteFriendApplyDao();

	public static SQLiteFriendApplyDao getInstance() {
		return instance;
	}

	public boolean saveApply(String siteUserId, String siteFriendId, String applyReason) throws SQLException {
		long startTime = System.currentTimeMillis();
		int result = 0;
		String sql = "INSERT INTO " + FRIEND_APPLY_TABLE
				+ "(site_user_id,site_friend_id,apply_reason,apply_time) VALUES(?,?,?,?);";

		PreparedStatement preState = SQLiteJDBCManager.getConnection().prepareStatement(sql);
		preState.setString(1, siteUserId);
		preState.setString(2, siteFriendId);
		preState.setString(3, applyReason);
		preState.setLong(4, System.currentTimeMillis());
		result = preState.executeUpdate();

		long endTime = System.currentTimeMillis();
		LogUtils.printDBLog(logger, endTime - startTime, +result, sql + siteUserId + "," + siteFriendId);
		return result > 0;
	}

	public boolean deleteApply(String siteUserId, String siteFriendId) throws SQLException {
		long startTime = System.currentTimeMillis();
		int result = 0;
		String sql = "DELETE FROM " + FRIEND_APPLY_TABLE + " WHERE site_user_id=? AND site_friend_id=?;";

		PreparedStatement preState = SQLiteJDBCManager.getConnection().prepareStatement(sql);
		preState.setString(1, siteUserId);
		preState.setString(2, siteFriendId);
		result = preState.executeUpdate();

		long endTime = System.currentTimeMillis();
		LogUtils.printDBLog(logger, endTime - startTime, ",result=" + result, sql + siteUserId);
		return result > 0;
	}

	public int getApplyCount(String siteUserId, String siteFriendId) throws SQLException {
		long startTime = System.currentTimeMillis();
		int num = 0;
		String sql = "SELECT COUNT(site_user_id) FROM " + FRIEND_APPLY_TABLE
				+ " WHERE site_user_id=? AND site_friend_id=?;";

		PreparedStatement preState = SQLiteJDBCManager.getConnection().prepareStatement(sql);
		preState.setString(1, siteUserId);
		preState.setString(2, siteFriendId);
		ResultSet rs = preState.executeQuery();
		num = rs.getInt(1);

		long endTime = System.currentTimeMillis();
		LogUtils.printDBLog(logger, endTime - startTime, num, sql + siteUserId + "," + siteFriendId);
		return num;
	}

	public int getApplyCount(String siteUserId) throws SQLException {
		long startTime = System.currentTimeMillis();
		int num = 0;
		String sql = "SELECT COUNT(site_user_id) FROM " + FRIEND_APPLY_TABLE + " WHERE site_user_id=?;";

		PreparedStatement preState = SQLiteJDBCManager.getConnection().prepareStatement(sql);
		preState.setString(1, siteUserId);
		ResultSet rs = preState.executeQuery();
		num = rs.getInt(1);

		long endTime = System.currentTimeMillis();
		LogUtils.printDBLog(logger, endTime - startTime, num, sql + siteUserId);
		return num;
	}

	/**
	 * 查询好友申请列表
	 * 
	 * @param siteUserId
	 * @return
	 * @throws SQLException
	 */
	public List<ApplyUserBean> queryApplyUsers(String siteUserId) throws SQLException {
		long startTime = System.currentTimeMillis();
		List<ApplyUserBean> applyUsers = new ArrayList<ApplyUserBean>();

		String sql = "SELECT a.site_friend_id,b.user_name,b.user_photo,a.apply_reason,MAX(a.apply_time) FROM "
				+ FRIEND_APPLY_TABLE + " AS a LEFT JOIN " + SQLConst.SITE_USER_PROFILE
				+ " AS b WHERE a.site_friend_id=b.site_user_id AND a.site_user_id=?;";

		PreparedStatement preState = SQLiteJDBCManager.getConnection().prepareStatement(sql);
		preState.setString(1, siteUserId);
		ResultSet rs = preState.executeQuery();
		while (rs.next()) {
			ApplyUserBean userBean = new ApplyUserBean();
			userBean.setUserId(rs.getString(1));
			userBean.setUserName(rs.getString(2));
			userBean.setUserPhoto(rs.getString(3));
			userBean.setApplyReason(rs.getString(4));
			userBean.setApplyTime(rs.getLong(5));
			applyUsers.add(userBean);
		}

		long endTime = System.currentTimeMillis();
		LogUtils.printDBLog(logger, endTime - startTime, applyUsers.size(), sql + siteUserId);
		return applyUsers;
	}
}
