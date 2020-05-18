
package snmp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.snmp4j.PDU;
import org.snmp4j.smi.OID;

/**
 * SNMP関連の定義
 *
 * @author SBC(SSL)
 *
 */
public final class SnmpDefinition {

	/*************************************
	 * デフォルト値
	 *************************************/
	// デフォルトポート番号
	public static final String DEFAULT_PORT="/161";

	// デフォルトタイムアウト
	public static final int DEFAULT_TIMEOUT=10000;

	// デフォルトリトライ
	public static final int DEFAULT_RETRAY=3;

	// リクエスト正常
	public static final int REQUSET_OK=0;

	/*************************************
	 * エラーコード
	 *************************************/
	// 結果正常
	public static final int SNMP_SUCCESS = PDU.noError;

	// エラー
	public static final int SNMP_INTERNALERROR = -1;

	// SNMP通信エラー
	public static final int SNMP_SNMPERROR = -2;

	// 応答無し
	public static final int SNMP_NORESPONSE = -3;

	// 不正なレスポンス
	public static final int SNMP_INVALIDRESPONSE = -4;

	// キャンセル
	public static final int SNMP_CANCEL = -5;

	// サポート外
	public static final int SNMP_NOTSUPPORTED = PDU.noSuchName;

	/*************************************
	 * 文字列
	 *************************************/
	public static final String RESULT_STR_SNMPERROR= "#SNMP_ERROR";

	public static final String RESULT_STR_INTERNALERROR= "#INTERNAL_ERROR";

	public static final String RESULT_STR_NORESPONSE= "#NO_RESPONSE";

	public static final String RESULT_STR_NOTSUPPORTED= "#N/A";

	public static final String RESULT_STR_IPADENTADDR= "SNMP_IPADENTADDR";

	/*************************************
	 * Response Listener種別
	 *************************************/
	public static enum ListenerType {
		Unicast,
		Broadcast
	}

	/*************************************
	 * OID
	 *************************************/

	/*
	 *  mib-2 : 1, 3, 6, 1, 2, 1
	 */

	public static final String GetNext_prtGeneralCurrentLocalization =
			"1.3.6.1.2.1.43.5.1.1.2";

	// シリアルNO
	public static final String Get_serialId =
			"1.3.6.1.2.1.43.5.1.1.17.1";

	public static final String GetNext_serialId =
			"1.3.6.1.2.1.43.5.1.1.17";

	// モデル名
	public static final String Get_modelName =
			"1.3.6.1.2.1.25.3.2.1.3.1";

	public static final String GetNext_modelName =
			"1.3.6.1.2.1.25.3.2.1.3";

	//ProductID
	public static final String Get_ProductId =
			"1.3.6.1.2.1.1.2.0";

	public static final String GetNext_ProductId =
			"1.3.6.1.2.1.1.2";


	/*************************************
	 * 給紙トレイ
	 *************************************/

	// 給紙トレイ- 手差しトレイ (manual(prtInputType)
	public static final String Get_inTray_manual =
			"1.3.6.1.2.1.43.8.2.1.2";

	// 給紙トレイ- 名前
	public static final String Get_inTray_name =
			"1.3.6.1.2.1.43.8.2.1.13";

	// 給紙トレイ- 説明
	public static final String Get_inTray_description =
			"1.3.6.1.2.1.43.8.2.1.18";

	// 給紙トレイ- モデル名
	public static final String Get_inTray_modelName =
			"1.3.6.1.2.1.43.8.2.1.15";

	// 給紙トレイ- 仮想トレイ
	public static final List<String> Get_inTray_virtual = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					Get_inTray_manual,
					Get_inTray_modelName, Get_inTray_name
			)
	));

	// 給紙トレイ- 容量(最大値)
	public static final String Get_inTray_capacity =
			"1.3.6.1.2.1.43.8.2.1.9";

	// 給紙トレイ- 残量の単位
	public static final String Get_inTray_unit =
			"1.3.6.1.2.1.43.8.2.1.8";

	// 給紙トレイ- 残量(現在値)
	public static final String Get_inTray_typical =
			"1.3.6.1.2.1.43.8.2.1.10";

	// 給紙トレイ- 残量(状態)
	public static final String Get_inTray_state = Get_inTray_typical;

	// 給紙トレイ-フィード方向選択長(送り方向の長さ)
	public static final String Get_inTray_MediaDimFeedDirChosen =
			"1.3.6.1.2.1.43.8.2.1.6";

	// 給紙トレイ-クロスフィード方向選択長(送り方向に対して直交した長さ)
	public static final String Get_inTray_MediaDimXFeedDirChosen =
			"1.3.6.1.2.1.43.8.2.1.7";

	// 給紙トレイ- メディアサイズ(用紙の縦長:AxB のAに相当)
	public static final String Get_inTray_mediaSizeHeight = Get_inTray_MediaDimXFeedDirChosen;

	// 給紙トレイ- メディアサイズ(用紙の横長:AxB のBに相当)
	public static final String Get_inTray_mediaSizeWidth = Get_inTray_MediaDimFeedDirChosen;

	// 給紙トレイ- メディアサイズ
	public static final List<String> Get_inTray_mediaSize = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					Get_inTray_MediaDimFeedDirChosen,
					Get_inTray_MediaDimXFeedDirChosen
			)
	));

	// 給紙トレイ- メディアサイズ名
	public static final List<String> Get_inTray_mediaSizeName = Get_inTray_mediaSize;

	// 給紙トレイ- メディアサイズ（ユニット）
	public static final String Get_inTray_mediaSize_unit =
			"1.3.6.1.2.1.43.8.2.1.3";

	// 給紙トレイ- メディアサイズ（名前）
	public static final List<String> Get_inTray_mediaSize_name =Get_inTray_mediaSize;

	// 給紙トレイ- メディアタイプ
	public static final String Get_inTray_mediaType =
			"1.3.6.1.2.1.43.8.2.1.21";

	// 給紙トレイ- メディア名
	public static final String Get_inTray_mediaName =
			"1.3.6.1.2.1.43.8.2.1.12";

	// 給紙トレイ(Walk)
	public static final String Walk_inTrayKey = Get_inTray_manual;


	/*************************************
	 * 排紙トレイ
	 *************************************/

	// 排紙トレイ
	public static final String Walk_outTrayKey =
			"1.3.6.1.2.1.43.9.2.1.2";

	// 排紙トレイ- 名前
	public static final String Get_outTray_name =
			"1.3.6.1.2.1.43.9.2.1.7";

	// 排紙トレイ- 説明
	public static final String Get_outTray_description =
			"1.3.6.1.2.1.43.9.2.1.12";

	// 排紙トレイ- モデル名
	public static final String Get_outTray_modelName =
			"1.3.6.1.2.1.43.9.2.1.9";

	// 排紙トレイ- 容量(最大値)
	public static final String Get_outTray_capacity =
			"1.3.6.1.2.1.43.9.2.1.4";

	// 排紙トレイ- 残量の単位
	public static final String Get_outTray_unit =
			"1.3.6.1.2.1.43.9.2.1.3";

	// 排紙トレイ- 残量(現在値)
	public static final String Get_outTray_typical =
			"1.3.6.1.2.1.43.9.2.1.5";

	// 排紙トレイ- 残量(状態)
	public static final String Get_outTray_state = Get_outTray_typical;

	// 排紙トレイ- stackingOrder
	public static final String Get_outTray_stackingOrder =
			"1.3.6.1.2.1.43.9.2.1.19";

	// 排紙トレイ- 排紙方向
	public static final String Get_outTray_deliveryOrientation =
			"1.3.6.1.2.1.43.9.2.1.20";


	/*************************************
	 * デバイス情報
	 *************************************/

	// 機器構成 - InstalledOptionsの値
	public static final String Get_configurations_InstalledOptions_value =
			"1.3.6.1.4.1.2385.1.1.1.2.1.14.1";

	// 機器構成 - DSKオプション装着済
	public static final String Get_configurations_InstalledOptions_dsk =
			"1.3.6.1.4.1.2385.1.1.1.2.1.14.1";

	// コードセット
	public static final List<String> Get_codeSet = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					"1.3.6.1.2.1.43.5.1.1.10.1",
					"1.3.6.1.2.1.43.7.1.1.4.1"
			)
	));

	// 設置場所 - 国
	public static final List<String> Get_location_country = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					"1.3.6.1.2.1.43.5.1.1.10.1",
					"1.3.6.1.2.1.43.7.1.1.3.1"
			)
	));

	// 設置場所 - 言語
	public static final List<String> Get_location_language = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList("1.3.6.1.2.1.43.5.1.1.10.1", "1.3.6.1.2.1.43.7.1.1.2.1")
	));

	// 設置場所 - 住所
	public static final String Get_location_address =
			"1.3.6.1.2.1.1.6.0";

	public static final String GetNext_location_address =
			"1.3.6.1.2.1.1.6";

	// FriendlyName
	public static final String Get_friendlyName =
			"1.3.6.1.2.1.1.5.0";

	public static final String GetNext_friendlyName =
			"1.3.6.1.2.1.1.5";

	// ContactAddress
	public static final String Get_contactAddress =
			"1.3.6.1.2.1.43.5.1.1.4.1";

	// 説明
	public static final String Get_description =
			"1.3.6.1.2.1.1.1.0";

	// エラーレベル
	public static final List<String> Get_errorLevel = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					"1.3.6.1.2.1.25.3.2.1.5.1",
					"1.3.6.1.2.1.25.3.5.1.1.1"
			)
	));

	// 警告
	public static final String Walk_alert =
			"1.3.6.1.2.1.43.18.1.1.2";

	// 警告 - 重大度
	public static final String Get_alert_severityLevel =
			"1.3.6.1.2.1.43.18.1.1.2";

	// アラートテーブル - TrainingLevel
	public static final String Get_alertList_trainingLevel =
			"1.3.6.1.2.1.43.18.1.1.3";

	// アラートテーブル - Group
	public static final String Get_alertList_group =
			"1.3.6.1.2.1.43.18.1.1.4";

	// アラートテーブル - GroupIndex
	public static final String Get_alertList_groupIndex =
			"1.3.6.1.2.1.43.18.1.1.5";

	// アラートテーブル - Location
	public static final String Get_alertList_location =
			"1.3.6.1.2.1.43.18.1.1.6";

	// 警告 - コード
	public static final String Get_alert_code =
			"1.3.6.1.2.1.43.18.1.1.7";

	// 警告 - 説明
	public static final String Get_alert_description =
			"1.3.6.1.2.1.43.18.1.1.8";

	// アラートテーブル - Time
	public static final String Get_alertList_time =
			"1.3.6.1.2.1.43.18.1.1.9";

	// サービスコード
	public static final String Walk_serviceCodeKey =
			"1.3.6.1.2.1.43.18.1.1.8";

	// サービスコード - value
	public static final String Get_serviceCode_value =
			"1.3.6.1.2.1.43.18.1.1.8";

	// MFP状態 - エラー状態
	public static final List<String> Get_statusSet = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					"1.3.6.1.2.1.25.3.2.1.5.1",
					"1.3.6.1.2.1.25.3.5.1.1.1",
					"1.3.6.1.2.1.25.3.5.1.2.1"
			)
	));

	public static final List<String> Get_statusSet2 = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					"1.3.6.1.2.1.25.3.2.1.5.1",
					"1.3.6.1.2.1.25.3.5.1.1.1",
					"1.3.6.1.2.1.25.3.5.1.2.1",
					"1.3.6.1.4.1.2385.1.1.3.2.1.6.1"
			)
	));

	// MFP状態 - その他のエラー状態(印刷不可)
	public static final String Get_statusSet_printerError =
			"1.3.6.1.2.1.25.3.2.1.5.1";

	// UpTime
	public static final String Get_upTime =
			"1.3.6.1.2.1.1.3.0";

	// engine - pagesPerMinitues
	public static final String Get_engine_pagesPerMinitues =
			"1.3.6.1.2.1.43.13.4.1.4.1.1";

	// memory - size
	public static final String Get_memory_size =
			"1.3.6.1.2.1.25.2.2.0";

	// trapSupported
	public static final String GetNExt_trapSupported =
			"1.3.6.1.6.3.12";

	// duplexModule - type
	public static final String Get_duplexModule_type =
			"1.3.6.1.2.1.43.13.4.1.9";

	// duplexModule - walk
	public static final String Walk_duplexModule = Get_duplexModule_type;

	// 排紙トレイ - デフォルト
	public static final String Get_outTrayDefault =
			"1.3.6.1.2.1.43.5.1.1.7.1";

	// 給紙トレイ - デフォルト
	public static final String Get_inTrayDefault =
			"1.3.6.1.2.1.43.5.1.1.6.1";

	//machineId
	public static final String Get_machineId =
			"1.3.6.1.4.1.2385.1.1.1.2.1.11.1";

	/*************************************
	 * トナー、廃トナー
	 *************************************/


	// marker- 容量(最大値)
	public static final String Get_marker_capacity =
			"1.3.6.1.2.1.43.11.1.1.8";

	// marker- 残量の単位
	public static final String Get_marker_unit =
			"1.3.6.1.2.1.43.11.1.1.7";

	// marker- 残量(現在値)
	public static final String Get_marker_typical =
			"1.3.6.1.2.1.43.11.1.1.9";

	// marker- 残量(状態)
	public static final String Get_marker_state = Get_marker_typical;

	// marker- type
	public static final String Get_marker_type =
			"1.3.6.1.2.1.43.11.1.1.5";

	// marker- color
	public static final List<String> Get_marker_color = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					"1.3.6.1.2.1.43.11.1.1.3 ",
					"1.3.6.1.2.1.43.12.1.1.4.1"
			)
	));

	// marker- description
	public static final String Get_marker_description =
			"1.3.6.1.2.1.43.11.1.1.6";

	// disposalMarker- 容量(最大値)
	public static final String Get_disposalMarker_capacity =Get_marker_capacity;

	// disposalMarker- 残量の単位
	public static final String Get_disposalMarker_unit =Get_marker_unit;

	// disposalMarker- 残量(現在値)
	public static final String Get_disposalMarker_typical =Get_marker_typical;

	// disposalMarker- 残量(状態)
	public static final String Get_disposalMarker_state =Get_disposalMarker_typical;

	// disposalMarker- type
	public static final String Get_disposalMarker_type =Get_marker_type;

	// marker- color
//	public static final String[] Get_disposalMarker_color =Get_marker_color;
	public static final List<String> Get_disposalMarker_color =Get_marker_color;

	// disposalMarker- description
	public static final String Get_disposalMarker_description =Get_marker_description;

	// marker (walk)
	public static final String Walk_marker = Get_marker_type;

	// disposalMarke(walk)r
	public static final String Walk_disposalMarker = Walk_marker;



	/*************************************
	 * カバー
	 *************************************/


	// カバー - 説明
	public static final String Get_cover_description =
			"1.3.6.1.2.1.43.6.1.1.2";

	// カバー - 状態
	public static final String Get_cover_status =
			"1.3.6.1.2.1.43.6.1.1.3";

	// カバー -walk
	public static final String Walk_cover =Get_cover_description;

	/*************************************
	 * フィニッシャー
	 *************************************/

	// フィニッシャー - タイプ
	public static final String Get_finishing_type =
			"1.3.6.1.2.1.43.31.1.1.4";

	// フィニッシャー - 説明
	public static final String Get_finishing_description =
			"1.3.6.1.2.1.43.31.1.1.5";

	// フィニッシャー- 残量の単位
	public static final String Get_finishing_unit =
			"1.3.6.1.2.1.43.31.1.1.6";

	// フィニッシャー- 容量(最大値)
	public static final String Get_finishing_capacity =
			"1.3.6.1.2.1.43.31.1.1.7";

	// フィニッシャー- 残量(現在値)
	public static final String Get_finishing_typical =
			"1.3.6.1.2.1.43.31.1.1.8";

	// フィニッシャー
	public static final String Walk_finishing = Get_finishing_type;

	/*************************************
	 * デバイスイメージ
	 *************************************/

	// デバイスイメージ- 使用可能フォーマット
	public static final String Get_allowedFormat =
			"1.3.6.1.4.1.2385.1.1.12.2.1.4.1";


	// デバイスイメージ- フォーマット
	public static final String Get_currentImageFormat =
			Get_allowedFormat;

	// デバイスイメージ- rawData
	public static final String Walk_allowedRawData =
			"1.3.6.1.4.1.2385.1.1.13.2.1.2.1";

	// デバイスイメージ- 幅
	// currentImage - width
	public static final String Get_currentImageWidth =
			"1.3.6.1.4.1.2385.1.1.12.2.1.5.1";

	// デバイスイメージ- 高さ
	// currentImage - height
	public static final String Get_currentImageHeight =
			"1.3.6.1.4.1.2385.1.1.12.2.1.6.1";


	// デバイスイメージ- サイズ(高さ x 幅)
	// currentImage - size
	public static final List<String> Get_currentImageSize = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					Get_currentImageHeight,  //高さ
					Get_currentImageWidth    //幅
			)
	));


	/*************************************
	 * カウンタ
	 *************************************/
	// カウンタ - Work用(value)
	public static final List<String> Get_counter_value = Collections.unmodifiableList(new ArrayList<String>(
			Arrays.asList(
					"1.3.6.1.2.1.43.10.2.1.5",		//index=0 powerOnCounter用
					"1.3.6.1.2.1.43.10.2.1.4"		//index=1 lifeCounter用
			)
	));


	// カウンタ - Work用(unit)
	public static final String Get_counter_unit =
			"1.3.6.1.2.1.43.10.2.1.3";

	// カウンタ - Work用
	public static final String Walk_counterKey = Get_counter_unit;


	/*************************************
	 * デバイステーブル情報
	 *************************************/

	// デバイステーブル - Index
	public static final String Get_deviceEntry_deviceIndex =
			"1.3.6.1.2.1.25.3.2.1.1";

	// デバイステーブル - type
	public static final String Get_deviceEntry_type =
			"1.3.6.1.2.1.25.3.2.1.2";

	// デバイステーブル - description
	public static final String Get_deviceEntry_description =
			"1.3.6.1.2.1.25.3.2.1.3";

	// デバイステーブル - deviceID
	public static final String Get_deviceEntry_deviceID =
			"1.3.6.1.2.1.25.3.2.1.4";

	// デバイステーブル - status
	public static final String Get_deviceEntry_status =
			"1.3.6.1.2.1.25.3.2.1.5";

	// デバイステーブル - errors
	public static final String Get_deviceEntry_errors =
			"1.3.6.1.2.1.25.3.2.1.6";

	// デバイステーブル
	public static final String Walk_deviceEntry = Get_deviceEntry_deviceIndex;

	/*************************************
	 * service
	 *************************************/

	//  service - DescriptionLanguage - family
	public static final String Get_service_DescriptionLanguage_family =
			"1.3.6.1.2.1.43.15.1.1.2";

	//  service - DescriptionLanguage - level
	public static final String Get_service_DescriptionLanguage_level =
			"1.3.6.1.2.1.43.15.1.1.3";

	//  service - DescriptionLanguage - langVersion
	public static final String Get_service_DescriptionLanguage_langVersion =
			"1.3.6.1.2.1.43.15.1.1.4";

	//  service - DescriptionLanguage - version
	public static final String Get_service_DescriptionLanguage_version =
			"1.3.6.1.2.1.43.15.1.1.6";

	//  service - DescriptionLanguage - OrientationDefault
	public static final String Get_service_DescriptionLanguage_OrientationDefault =
			"1.3.6.1.2.1.43.15.1.1.7";

	//  service - DescriptionLanguage - description
	public static final String Get_service_DescriptionLanguage_Description =
			"1.3.6.1.2.1.43.15.1.1.5";

	// service -DescriptionLanguage - walk
	public static final String Walk_service_DescriptionLanguage =
			Get_service_DescriptionLanguage_family;


	//  service - mediaPath - type
	public static final String Get_service_mediaPath_type =
			"1.3.6.1.2.1.43.13.4.1.9";


	// service - mediaPath - walk
	public static final String Walk_service_mediaPath =
			Get_service_mediaPath_type;


	//  service - channel - descriptionLanguageDefault
	public static final String Get_service_channel_descriptionLanguageDefault =
			"1.3.6.1.2.1.43.14.1.1.5";

	//  service - channel - mediaPathDefault
	public static final String Get_service_channel_mediaPathDefault =
			"1.3.6.1.2.1.43.5.1.1.9.1";

	// service - channel - walk
	public static final String Walk_service_channel =
			Get_service_channel_descriptionLanguageDefault;


	/*************************************
	 * interface
	 *************************************/
	// IP
	// IP - address
	public static final String Get_ip_address =
		"1.3.6.1.2.1.4.20.1.1";

	// IP - ethernetId (id of ethernetList)
	public static final String Get_ip_ethernetId =
		"1.3.6.1.2.1.4.20.1.2";

	// IP - subnetMask
	public static final String Get_ip_subnetMask =
		"1.3.6.1.2.1.4.20.1.3";

	// IP - defaultRoute
	public static final String Get_ip_defaultRoute =
		"1.3.6.1.2.1.4.21.1.7.0.0.0.0";

	public static final String Walk_ip = Get_ip_address;


	// Ethernet - address
	public static final String Get_ethernet_address =
		"1.3.6.1.2.1.2.2.1.6";


	// Ethernet - type
	public static final String Get_ethernet_type =
		"1.3.6.1.2.1.2.2.1.3";

	// Ethernet -walk
	public static final String  Walk_ethernet = Get_ethernet_address;

	/**
	 * SNMPリクエスト
	 */
	// get
	public static final String SNMP_REQUEST_GET = "get";

	// getNext
	public static final String SNMP_REQUEST_GETNEXT = "getNext";

	/**
	 * 検索用OIDリスト
	 * command:GetNext
	 */
	public static final List<OID> DiscoveryOids = Collections.unmodifiableList(new ArrayList<OID>(Arrays.asList(
			new OID(SnmpDefinition.GetNext_prtGeneralCurrentLocalization),
			new OID(SnmpDefinition.GetNext_serialId),
			new OID(SnmpDefinition.GetNext_ProductId),
			new OID(SnmpDefinition.GetNext_modelName),
			new OID(SnmpDefinition.GetNext_friendlyName),
			new OID(SnmpDefinition.GetNext_location_address)
			)));

	public static final List<OID> DiscoveryResponseOids = Collections.unmodifiableList(new ArrayList<OID>(Arrays.asList(
			new OID(SnmpDefinition.GetNext_prtGeneralCurrentLocalization),
			new OID(SnmpDefinition.Get_serialId),
			new OID(SnmpDefinition.Get_ProductId),
			new OID(SnmpDefinition.Get_modelName),
			new OID(SnmpDefinition.Get_friendlyName),
			new OID(SnmpDefinition.Get_location_address)
			)));
}

