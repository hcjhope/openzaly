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
package com.akaxin.site.business.impl.tai;

import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akaxin.common.command.Command;
import com.akaxin.common.command.CommandResponse;
import com.akaxin.common.constant.CommandConst;
import com.akaxin.common.constant.ErrorCode2;
import com.akaxin.common.crypto.HashCrypto;
import com.akaxin.common.crypto.RSACrypto;
import com.akaxin.proto.core.ConfigProto;
import com.akaxin.proto.core.CoreProto;
import com.akaxin.proto.core.UserProto;
import com.akaxin.proto.site.ApiSiteConfigProto;
import com.akaxin.proto.site.ApiSiteLoginProto;
import com.akaxin.proto.site.ApiSiteRegisterProto;
import com.akaxin.site.business.dao.SiteConfigDao;
import com.akaxin.site.business.dao.SiteLoginDao;
import com.akaxin.site.business.dao.UserProfileDao;
import com.akaxin.site.business.impl.AbstractRequest;
import com.akaxin.site.business.impl.site.SiteConfig;
import com.akaxin.site.business.impl.site.UserPhone;
import com.akaxin.site.business.impl.site.UserUic;
import com.akaxin.site.storage.bean.SimpleUserBean;
import com.akaxin.site.storage.bean.UserDeviceBean;
import com.akaxin.site.storage.bean.UserProfileBean;
import com.akaxin.site.storage.bean.UserSessionBean;

/**
 * 获取站点信息 <br>
 * 执行注册&&登陆
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2017-10-17 18:14:10
 */
public class ApiSiteService extends AbstractRequest {
	private static final Logger logger = LoggerFactory.getLogger(ApiSiteService.class);
	private static final int DEFAULT_PORT = 2021;

	/**
	 * <pre>
	 * 		api.site.config
	 * 		获取站点配置信息
	 * </pre>
	 * 
	 * @param command
	 * @return
	 */
	public CommandResponse config(Command command) {
		CommandResponse commandResponse = new CommandResponse().setAction(CommandConst.ACTION_RES);
		ErrorCode2 errCode = ErrorCode2.ERROR;
		try {
			Map<Integer, String> configMap = SiteConfig.getConfigMap();
			logger.info("api.site.config cmd={} ", command.toString());

			if (configMap != null) {
				ConfigProto.SiteConfig.Builder configBuilder = ConfigProto.SiteConfig.newBuilder();
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_ADDRESS_VALUE))) {
					configBuilder.setSiteAddress(configMap.get(ConfigProto.ConfigKey.SITE_ADDRESS_VALUE));
				}
				if (StringUtils.isNumeric(configMap.get(ConfigProto.ConfigKey.SITE_PORT_VALUE))) {
					configBuilder.setSitePort(Integer.valueOf(configMap.get(ConfigProto.ConfigKey.SITE_PORT_VALUE)));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_NAME_VALUE))) {
					configBuilder.setSiteName(configMap.get(ConfigProto.ConfigKey.SITE_NAME_VALUE));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_LOGO_VALUE))) {
					configBuilder.setSiteLogo(configMap.get(ConfigProto.ConfigKey.SITE_LOGO_VALUE));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_VERSION_VALUE))) {
					configBuilder.setSiteVersion(configMap.get(ConfigProto.ConfigKey.SITE_VERSION_VALUE));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_INTRODUCTION_VALUE))) {
					configBuilder.setSiteIntroduction(configMap.get(ConfigProto.ConfigKey.SITE_INTRODUCTION_VALUE));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.PIC_SIZE_VALUE))) {
					configBuilder.setPicSize(configMap.get(ConfigProto.ConfigKey.PIC_SIZE_VALUE));
				}
				if (StringUtils.isNumeric(configMap.get(ConfigProto.ConfigKey.REGISTER_WAY_VALUE))) {
					configBuilder
							.setRegisterWay(Integer.valueOf(configMap.get(ConfigProto.ConfigKey.REGISTER_WAY_VALUE)));
				}
				ApiSiteConfigProto.ApiSiteConfigResponse response = ApiSiteConfigProto.ApiSiteConfigResponse
						.newBuilder().setSiteConfig(configBuilder.build()).build();
				commandResponse.setParams(response.toByteArray());
				errCode = ErrorCode2.SUCCESS;
			}

		} catch (Exception e) {
			errCode = ErrorCode2.ERROR_SYSTEMERROR;
			logger.info("api.site.config execute error.", e);
		}
		logger.info("api.site.config result={}", errCode.toString());
		return commandResponse.setErrCode2(errCode);
	}

	public CommandResponse register(Command command) {
		CommandResponse commandResponse = new CommandResponse().setAction(CommandConst.ACTION_RES);
		ErrorCode2 errorCode = ErrorCode2.ERROR;
		try {
			ApiSiteRegisterProto.ApiSiteRegisterRequest registerRequest = ApiSiteRegisterProto.ApiSiteRegisterRequest
					.parseFrom(command.getParams());
			// 这里需要验证邀请码，如果有需要
			String userIdPubk = registerRequest.getUserIdPubk();
			String siteUserId = UUID.randomUUID().toString();// siteUserId保证各站不同
			String userName = registerRequest.getUserName();
			String userPhoto = registerRequest.getUserPhoto();
			String userUic = registerRequest.getUserUic();
			String applyInfo = registerRequest.getApplyInfo();
			String phoneToken = registerRequest.getPhoneToken();
			String phoneId = null;// 通过phoneCod

			logger.info("api.site.register siteUserId={} cmd={} request={}", siteUserId, command.toString(),
					registerRequest.toString());

			if (StringUtils.isAnyEmpty(userIdPubk, userName)) {
				errorCode = ErrorCode2.ERROR_PARAMETER;
				logger.error("");
				return commandResponse.setErrCode2(errorCode);
			}

			// 判断站点的注册方式
			ConfigProto.RegisterWay regWay = SiteConfig.getRegisterWay();

			logger.info("api.site.register register way={}", regWay);

			switch (regWay) {
			case USERUIC:
				logger.info("注册方式：邀请码注册");
				if (!UserUic.getInstance().checkUic(userUic, siteUserId)) {
					errorCode = ErrorCode2.ERROR_REGISTER_UIC;
					return commandResponse.setErrCode2(errorCode);
				}
				break;
			case REALNAME:
				logger.info("注册方式：实名注册");
				if (StringUtils.isNotBlank(phoneToken)) {
					phoneId = UserPhone.getInstance().getPhoneIdFromPlatform(phoneToken);
					logger.info("实名注册，站点获取手机号：{}", phoneId);
					if (!StringUtils.isNotBlank(phoneId)) {
						errorCode = ErrorCode2.ERROR_REGISTER_PHONEID;
						return commandResponse.setErrCode2(errorCode);
					}
				}
				break;
			case ANONYMOUS:
				logger.info("注册方式：匿名注册");
				break;
			case UNRECOGNIZED:
				break;
			}

			UserProfileBean regBean = new UserProfileBean();
			regBean.setSiteUserId(siteUserId);
			regBean.setUserIdPubk(userIdPubk);
			regBean.setUserName(userName);
			regBean.setApplyInfo(applyInfo);
			regBean.setUserPhoto(userPhoto);
			regBean.setPhoneId(phoneId);
			regBean.setUserStatus(UserProto.UserStatus.NORMAL_VALUE);
			regBean.setRegisterTime(System.currentTimeMillis());

			if (SiteLoginDao.getInstance().registerUser(regBean)) {
				ApiSiteRegisterProto.ApiSiteRegisterResponse response = ApiSiteRegisterProto.ApiSiteRegisterResponse
						.newBuilder().setSiteUserId(siteUserId).build();
				commandResponse.setParams(response.toByteArray());
				errorCode = ErrorCode2.SUCCESS;

			} else {
				errorCode = ErrorCode2.ERROR_REGISTER_SAVEPROFILE;
			}

			if (ErrorCode2.SUCCESS == errorCode) {
				// 注册成功，需要做一个管理员身份验证
				justForAdminUser(siteUserId, command.getHeader());
			}
		} catch (Exception e) {
			errorCode = ErrorCode2.ERROR_SYSTEMERROR;
			logger.error("api.site.register error.", e);
		}
		logger.info("api.site.register result={}", errorCode.toString());
		return commandResponse.setErrCode2(errorCode);
	}

	private void justForAdminUser(String siteUserId, Map<Integer, String> header) {
		// 如果站点没有管理员
		if (SiteConfig.hasNoAdminUser()) {
			logger.info("user first time to register site server ,set it as admin:{} map:{}", siteUserId, header);
			SiteConfigDao.getInstance().updateSiteConfig(ConfigProto.ConfigKey.SITE_ADMIN_VALUE, siteUserId);
			if (header != null) {
				String host = header.get(CoreProto.HeaderKey.CLIENT_REQUEST_SERVER_HOST_VALUE);
				if (StringUtils.isNotEmpty(host)) {
					SiteConfigDao.getInstance().updateSiteConfig(ConfigProto.ConfigKey.SITE_ADDRESS_VALUE, host);
					SiteConfigDao.getInstance().updateSiteConfig(ConfigProto.ConfigKey.SITE_NAME_VALUE, host);
				}
				String port = header.get(CoreProto.HeaderKey.CLIENT_REQUEST_SERVER_HOST_VALUE);
				if (StringUtils.isNotBlank(port)) {
					port = "" + DEFAULT_PORT;
					SiteConfigDao.getInstance().updateSiteConfig(ConfigProto.ConfigKey.SITE_PORT_VALUE, port);
				}
			}
			SiteConfig.updateConfig();
		}
	}

	/**
	 * 执行用户登陆站点行为
	 * 
	 * @param command
	 * @return
	 */
	public CommandResponse login(Command command) {
		CommandResponse commandResponse = new CommandResponse().setAction(CommandConst.ACTION_RES);
		ErrorCode2 errCode = ErrorCode2.ERROR;
		try {
			ApiSiteLoginProto.ApiSiteLoginRequest loginRequest = ApiSiteLoginProto.ApiSiteLoginRequest
					.parseFrom(command.getParams());

			String userIdPubk = loginRequest.getUserIdPubk();
			String userIdSignBase64 = loginRequest.getUserIdSignBase64();
			String userDeviceIdPubk = loginRequest.getUserDeviceIdPubk();
			String userDeviceIdSignBase64 = loginRequest.getUserDeviceIdSignBase64();
			String userDeviceName = loginRequest.getUserDeviceName();
			String userToken = loginRequest.getUserToken();

			logger.info("user_id_pubk={}", userIdPubk);
			logger.info("userIdSignBase64={}", userIdSignBase64);
			logger.info("device_id_pubk={}", userDeviceIdPubk);
			logger.info("userDeviceIdSignBase64={}", userDeviceIdSignBase64);

			if (StringUtils.isAnyEmpty(userIdPubk, userIdSignBase64)) {
				errCode = ErrorCode2.ERROR2_LOGGIN_USERID_EMPTY;
				return commandResponse.setErrCode2(errCode);
			}

			if (StringUtils.isAnyEmpty(userDeviceIdPubk, userDeviceIdSignBase64)) {
				errCode = ErrorCode2.ERROR2_LOGGIN_DEVICEID_EMPTY;
				return commandResponse.setErrCode2(errCode);
			}

			PublicKey userPubKey = RSACrypto.getRSAPubKeyFromPem(userIdPubk);// 个人身份公钥，解密Sign签名，解密Key
			Signature userSign = Signature.getInstance("SHA512withRSA");
			userSign.initVerify(userPubKey);
			userSign.update(userIdPubk.getBytes());// 原文
			boolean userSignResult = userSign.verify(Base64.getDecoder().decode(userIdSignBase64));
			logger.info("userSignResult={}", userSignResult);

			if (userSignResult) {
				Signature userDeviceSign = Signature.getInstance("SHA512withRSA");
				userDeviceSign.initVerify(userPubKey);
				userDeviceSign.update(userDeviceIdPubk.getBytes());// 原文
				userSignResult = userDeviceSign.verify(Base64.getDecoder().decode(userDeviceIdSignBase64));
			}
			logger.info("deviceSignResult={}", userSignResult);

			// 用户身份校验成功，方可执行登陆操作
			if (userSignResult) {
				// 判断用户，是否已经注册
				SimpleUserBean subean = UserProfileDao.getInstance().getSimpleProfileByPubk(userIdPubk);
				if (subean == null || StringUtils.isEmpty(subean.getUserId())) {
					logger.info("Login Error:	Never Register On Site!");
					errCode = ErrorCode2.ERROR2_LOGGIN_NOREGISTER;// 未注册,告知用户执行注册行为
					return commandResponse.setErrCode2(errCode);
				}

				if (subean.getUserStatus() == UserProto.UserStatus.SEALUP_VALUE) {
					logger.info("Login Error:	Seal Up user no permision");
					errCode = ErrorCode2.ERROR2_LOGGIN_SEALUPUSER;// 禁封用户禁止登陆
					return commandResponse.setErrCode2(errCode);
				}

				String siteUserId = subean.getUserId();
				String deviceId = HashCrypto.MD5(userDeviceIdPubk);

				logger.info("Login: Check User, siteUserId={} deviceId={}", siteUserId, deviceId);

				// 保存设备信息
				UserDeviceBean deviceBean = new UserDeviceBean();
				deviceBean.setDeviceId(deviceId);
				deviceBean.setDeviceName(userDeviceName);
				deviceBean.setSiteUserId(siteUserId);
				deviceBean.setUserDevicePubk(userDeviceIdPubk);
				deviceBean.setUserToken(userToken);
				deviceBean.setActiveTime(System.currentTimeMillis());
				deviceBean.setAddTime(System.currentTimeMillis());

				boolean loginResult = SiteLoginDao.getInstance().updateUserDevice(deviceBean);

				if (!loginResult) {// 更新失败，则重新保存数据
					loginResult = SiteLoginDao.getInstance().saveUserDevice(deviceBean);
				}

				logger.info("Login:save device result={} deviceBean={}", loginResult, deviceBean.toString());

				if (loginResult) {
					// 生成session
					String sessionId = UUID.randomUUID().toString();

					UserSessionBean sessionBean = new UserSessionBean();
					sessionBean.setLoginTime(System.currentTimeMillis());
					sessionBean.setSiteUserId(siteUserId);
					sessionBean.setOnline(true);
					sessionBean.setSessionId(sessionId);
					sessionBean.setDeviceId(deviceId);
					sessionBean.setLoginTime(System.currentTimeMillis());// 上次登陆(auth)时间

					// 登陆信息入库,保存session
					logger.info("Login:sessionId={}", sessionId);
					loginResult = loginResult && SiteLoginDao.getInstance().saveUserSession(sessionBean);
					logger.info("Login:save session result={}", loginResult);

					if (loginResult) {
						ApiSiteLoginProto.ApiSiteLoginResponse response = ApiSiteLoginProto.ApiSiteLoginResponse
								.newBuilder().setSiteUserId(siteUserId).setUserSessionId(sessionId).build();
						commandResponse.setParams(response.toByteArray());
						errCode = ErrorCode2.SUCCESS;
					} else {
						errCode = ErrorCode2.ERROR2_LOGGIN_UPDATE_SESSION;
					}
				} else {
					errCode = ErrorCode2.ERROR2_LOGGIN_UPDATE_DEVICE;
				}
			} else {
				errCode = ErrorCode2.ERROR2_LOGGIN_ERRORSIGN;
			}
		} catch (Exception e) {
			errCode = ErrorCode2.ERROR_SYSTEMERROR;
			logger.error("user login site exception.", e);
		}
		logger.info("api.site.login result={}", errCode.toString());
		return commandResponse.setErrCode2(errCode);
	}

}
