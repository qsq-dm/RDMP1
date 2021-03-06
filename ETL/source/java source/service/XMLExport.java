package com.dbs.sg.DTE12.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import com.dbs.sg.DTE12.common.FileOperator;
import com.dbs.sg.DTE12.common.LoadConfigXml;
import com.dbs.sg.DTE12.common.Logger;
import com.dbs.sg.DTE12.config.OutputXMLFileList;
import com.dbs.sg.DTE12.config.Batchlist.batch;
import com.dbs.sg.DTE12.config.OutputXMLFileList.file;
import com.dbs.sg.DTE12.loganalysis.LogAnalysisDAO;
import com.dbs.sg.DTE12.loganalysis.PaginationController;

public class XMLExport {
	/**
	 * Logger
	 */
	private static Logger logger;

	public static String propPath = "xmlexport.properties";

	public static String propTranslatePath = "xmltranslate.properties";

	public static final String COMMERCIAL = "COMMERCIAL";

	public static final String FACILITY = "FACILITY";

	public static final String OTC = "OTC";

	public static final String COUNTERPARTY = "COUNTERPARTY";

	public static final String CURRENCYACA = "CURRENCYACA";

	public static final String CURRENCYACLM = "CURRENCYACLM";

	public static final String FINANCIALSTATEMENT = "FINANCIALSTATEMENT";

	public static final String MITIGANT = "MITIGANT";

	public static final String FINANCIALSTATEMENTRC = "FINANCIALSTATEMENTRC";

	public static String lineSep = System.getProperty("line.separator");
	private StringBuffer sb;

	private LogAnalysisDAO dao = null;

	// private LogAnalysisDAO dao1 = null;

	private LoadConfigXml config;

	private Properties propxmlexport;

	private Map mapXMLEscape;

	private Map cacheMap;

	private Map cacheControlMap;

	private String batchId;

	private String configPath;

	private String subType;
	private int databoxSize;
	private int pageSize;
	private long rowCount;
	private String databoxPath;
	private String Xmlheader;
	private String trailer;
	private String databoxAppTag;
	private String databoxAppTagEnd;
	public XMLExport(String configPath, String batchId) throws Exception {
		this.config = LoadConfigXml.getConfig(configPath);
		this.batchId = batchId;
		this.databoxSize = Integer.parseInt(config.getDataboxSize());
		this.pageSize = Integer.parseInt(config.getPageSize());
		if (configPath.endsWith(LoadConfigXml.file_separator))
			this.configPath = configPath;
		else
			this.configPath = configPath + LoadConfigXml.file_separator;
		this.init();
	}

	private void init() throws Exception {
		propxmlexport = new Properties();
		propxmlexport.load(new FileInputStream(configPath + propPath));
		Properties propxmltranslate = new Properties();
		propxmltranslate.load(new FileInputStream(configPath
				+ propTranslatePath));
		mapXMLEscape = new LinkedHashMap();
		mapXMLEscape.put("&", "&amp;");
		Enumeration e = propxmltranslate.propertyNames();
		while (e.hasMoreElements()) {
			String node = (String) e.nextElement();
			if (!mapXMLEscape.containsKey(node))
				mapXMLEscape.put("".equalsIgnoreCase(node) ? " " : node,
						propxmltranslate.getProperty(node));
		}

		initializeDAO();
	}

	public LoadConfigXml getLoadConfigXml() {
		return config;
	}

	public Properties getPropxmlexport() {
		return propxmlexport;
	}

	public Map getMapXMLEscape() {
		return mapXMLEscape;
	}

	public void generateOutputXML() throws Exception {
		try {
			batch batch = getLoadConfigXml().getBatch(batchId);
			String basepath = getLoadConfigXml().getBasePath();
			if (batch == null)
				throw new Exception("Can not find the configure of " + batchId);
			OutputXMLFileList oList = batch.getOutputXmlFileList();
			for (int i = 0; i < oList.getFileCount(); i++) {
				file f = oList.getFile(i);
				// if (COMMERCIAL.equalsIgnoreCase(f.getType())
				// || FACILITY.equalsIgnoreCase(f.getType())
				// || OTC.equalsIgnoreCase(f.getType())
				// || COUNTERPARTY.equalsIgnoreCase(f.getType())
				// || CURRENCYACA.equalsIgnoreCase(f.getType())
				// || CURRENCYACLM.equalsIgnoreCase(f.getType())
				// || FINANCIALSTATEMENT.equalsIgnoreCase(f.getType())
				// || FINANCIALSTATEMENTRC.equalsIgnoreCase(f.getType())
				// || MITIGANT.equalsIgnoreCase(f.getType())) {
				this.cacheControlMap = null;
				this.cacheMap = null;
				deleteDatabox(basepath + f.getFileName());
				generateDatabox(batch.getId(), basepath + f.getFileName(), f
						.getType(), f.getSubtype());
				// } else {
				// // logger.error("Error databox Type["+ f.getType() +"] in
				// // Batch=" + batchId + ",outputfilename=" +
				// // f.getFileName() + ".");
				// throw new Exception("Error databox Type[" + f.getType()
				// + "] in Batch=" + batchId + ",outputfilename="
				// + f.getFileName() + ".");
				// }
			}
		} catch (SQLException e) {
			/*
			 * logger.error("An error occured in generateOutputXML(batchId=" +
			 * batchId + ").", e);
			 */
			throw new Exception(
					"An error occured in generateOutputXML(batchId=" + batchId
							+ ").", e);
		} finally {
			this.closeDAO();
		}
	}

	private void deleteDatabox(String filePath) {
		int count = 1;
		while (true) {
			String str = filePath;
			if (count > 1)
				str = filePath.substring(0, filePath.lastIndexOf(".")) + "_" + count + ".xml";
			File f = new File(str);
			if (f.exists()) {
				f.delete();
				count++;
			} else
				break;
		}
	}
	private void generateDatabox(String batchId, String filePath, String type, String subType)
			throws Exception {
		logger.info("generateDatabox(batchId=" + batchId + ",filePath="
				+ filePath + ",type=" + type + ",subType=" + subType
				+ ") - begin.");
		String databoxtype = type.toLowerCase();
		this.subType = subType;
		// header
		Xmlheader = getHeader(null);
		if (Xmlheader == null) {
			logger
					.error("the value of parameter[_head],please check the configure file[DTE12/config/xmloutput.properties].");
			return;
		}
		/*
		 * String versionNo = this.getVersion(databoxtype); if (versionNo ==
		 * null) { logger .error("the value of parameter[" + databoxtype +
		 * ".version],please check the configure
		 * file[DTE12/config/xmloutput.properties]."); //return; } header =
		 * header.replaceFirst("&&version", versionNo);
		 */
		trailer = this.getTrailer(null);
		if (trailer == null) {
			logger
					.error("the value of parameter[_trailer],please check the configure file[DTE12/config/xmloutput.properties].");
			return;
		}
		this.iotime = 0;
		this.sqltime = 0;
		this.getvaluemaptime = 0;
		this.processxmltime = 0;
		sb = new StringBuffer();
		// sb.append(header);
		// process XML
		long t1 = System.currentTimeMillis();
		this.processXML(databoxtype, 0, null, filePath,
				new String[] { batchId });
		// trailer
		// sb.append(trailer);
		// System.out.println(sb.toString());
		long t2 = System.currentTimeMillis();
//		int page = (int) this.rowCount / this.databoxSize + 1;
//		if (page > 1)
//			databoxPath = filePath.substring(0, filePath.lastIndexOf(".")) + "_" + page + ".xml";
//		else
//			databoxPath = filePath;
		this.getDataboxPath(filePath);
		File f = new File(databoxPath);
		if (!f.exists()) {
			FileOperator.Write(databoxPath,
					databoxAppTag.toString().getBytes(), true);
		}
		FileOperator.Write(databoxPath, (sb.toString() + this.trailer)
				.getBytes(), true);
		this.iotime += System.currentTimeMillis() - t2;
		processxmltime += System.currentTimeMillis() - t1;
		logger.info("generateDatabox(batchId=" + batchId + ",filePath="
				+ filePath + ",type=" + type + ",subType=" + subType
				+ ") - end. processxmltime=" + this.processxmltime
				+ ",getvaluemaptime=" + this.getvaluemaptime + ",iotime="
				+ this.iotime + ",sqltime=" + this.sqltime + ",rowcount="
				+ this.rowCount);
	}

	private String composeQuerySql(String note, Object[] params)
			throws FileNotFoundException, IOException {
		String tbl = getSqlTble(note);
		String fields = getSqlFields(note);
		String order = getSqlOrder(note);
		String sqlwhere = getSqlWhere(note);
		StringBuffer sb = new StringBuffer();
		sb.append("select ");
		sb.append(fields);
		sb.append(" from ");
		sb.append(tbl);
		if (sqlwhere != null && !"".equals(sqlwhere)) {
			for (int i = 0; i < params.length; i++) {
				sqlwhere = sqlwhere.replaceFirst("&&param" + (i + 1),
						this.batchId);
			}
			sb.append(" where " + sqlwhere);
		}
		if (order != null && !"".equals(order)) {
			sb.append(" ORDER BY " + order);
		}
		return sb.toString();
	}

	private List getNextResultSet(String note, Object[] params)
			throws FileNotFoundException, IOException, SQLException {
		List result = new LinkedList();
		long t1 = System.currentTimeMillis();
		if (cacheControlMap == null)
			cacheControlMap = new LinkedHashMap();
		if (cacheControlMap.containsKey(note)) {
			PaginationController pc = (PaginationController) cacheControlMap
					.get(note);
			if (pc.hasNext()) {
				ResultSet rs = dao.getResultSet(pc);
				String fields = getSqlFields(note);
				String[] fdArray = fields.split(",");
				while (rs.next()) {
					Map record = new LinkedHashMap();
					for (int i = 0; i < fdArray.length; i++) {
						String f = fdArray[i].trim();
						f = f.substring(f.indexOf(".") + 1);
						record.put(f, rs.getString(f));
					}
					result.add(record);
				}
				dao.cleanUp();
				this.sqltime += System.currentTimeMillis() - t1;
				// System.out.println(note + "[" + pc.rowCount + "," +
				// pc.rowCurr +
				// "]:sqltime" + this.sqltime);
			}
		} else {
			String sql = composeQuerySql(note, params);
			PaginationController pc = dao.getOneSqlResultSetPageable(sql,
					this.pageSize);
			cacheControlMap.put(note, pc);
			this.sqltime += System.currentTimeMillis() - t1;
			// System.out.println(note + "[" + pc.rowCount + "," + pc.rowCurr +
			// "]:sqltime" + this.sqltime);
			return getNextResultSet(note, params);
		}
		return result;
	}

	private long sqltime;

	private long getvaluemaptime;

	private Map getNextResult(String note, Object[] params)
			throws FileNotFoundException, IOException, SQLException {
		String sqlkey = getSqlkey(note);
		Map valueMap = null;
		if (cacheMap == null)
			cacheMap = new LinkedHashMap();
		long t1 = System.currentTimeMillis();
		if (cacheMap.containsKey(note)) {
			List list = (List) cacheMap.get(note);
			if (list.size() == 0) {
				list = getNextResultSet(note, params);
				if (list.size() == 0) {
					getvaluemaptime += System.currentTimeMillis() - t1;
					return null;
				}
				cacheMap.put(note, list);
			}
			// change by Jason 20080327 while main table seqno is bigger than
			// sub table seqno, go on dealing next sub table record.
			while (list.size() > 0) {
				valueMap = (Map) list.remove(0);
				if (sqlkey != null && !"".equals(sqlkey)) {
					getvaluemaptime += System.currentTimeMillis() - t1;
					return valueMap;
				} else {
					if (params[0].equals((String) valueMap.get("SEQNO"))) {
						getvaluemaptime += System.currentTimeMillis() - t1;
						return valueMap;
					} else {
						if (Integer.parseInt((String) params[0]) < Integer
								.parseInt((String) valueMap.get("SEQNO"))) {
							list.add(0, valueMap);
							getvaluemaptime += System.currentTimeMillis() - t1;
							return null;
						}
					}
				}
			}
			return getNextResult(note, params);
			// String v = null;
			// String fd = sqlwhere.substring(0, sqlwhere.indexOf(">="));
			/*
			 * long t2 = System.currentTimeMillis(); for (int i = 0; i <
			 * list.size(); i++) { Map map = (Map) list.get(i); v =
			 * (String)map.get("SEQNO"); if (params[0].equals(v)) { valueMap =
			 * map; list.remove(i); processxmltime +=
			 * System.currentTimeMillis()-t2; getvaluemaptime +=
			 * System.currentTimeMillis()-t1; return valueMap; } }
			 * processxmltime += System.currentTimeMillis()-t2;
			 */
			/*
			 * if (v !=null && v.compareTo(params[0]) > 0){ getvaluemaptime +=
			 * System.currentTimeMillis()-t1; return null; } else {
			 * list.clear(); list = getNextResultSet(note, params); if
			 * (list.size() == 0){ getvaluemaptime +=
			 * System.currentTimeMillis()-t1; return null; } cacheMap.put(note,
			 * list); return getNextResult(note, params); }
			 */
		} else {
			List list = getNextResultSet(note, params);
			if (list.size() == 0) {
				getvaluemaptime += System.currentTimeMillis() - t1;
				return null;
			}
			cacheMap.put(note, list);
			return getNextResult(note, params);
		}
	}

	private long processxmltime;

	private void processXML(String note, int depth, Map valueMap,
			String filePath, Object[] params) throws Exception {
		logger.debug("processXML(note=" + note + ",depth=" + depth
				+ ",valueMap=" + valueMap + ",filepath=" + filePath
				+ ",params=" + Arrays.asList(params) + ") - begin.");
		note = note.trim();
		String header = this.getHeader(note);
		String child = this.getChild(note);
		String value = this.getValue(note);
		String sql = this.getSqlTble(note);
		String sqlkey = this.getSqlkey(note);
		Object[] paramKeyArray = new Object[] {};
		if (sqlkey != null) {
			paramKeyArray = sqlkey.split(",");
			String parentNote = note.substring(0, note.lastIndexOf("."));
			this.databoxAppTag = this.Xmlheader + sb.toString();
			this.databoxAppTagEnd = lineSep
					+ this.getXMLElementEnd(this.getHeader(parentNote))
					+ this.trailer;
			sb = new StringBuffer();
			this.rowCount = 0;
		}
		logger.debug("processXML()- sql=" + sql + ",paramArray="
				+ Arrays.asList(paramKeyArray));
		if (sql != null) {
			// for (int i = 0; i < params.length; i++) {
			// sql = sql.replaceFirst("&&param" + (i + 1), (String) params[i]);
			// }
			// LogAnalysisDAO daolocal;
			// if (depth <= 1)
			// daolocal = this.dao;
			// else
			// daolocal = this.dao1;
			// PaginationController pc =
			// daolocal.getOneSqlResultSetPageable(sql,
			// 5000);
			// while (pc.hasNext()) {
			while ((valueMap = this.getNextResult(note, params)) != null) {
				// ResultSet rstmp = daolocal.getResultSet(pc);
				// while (rstmp != null && rstmp.next()) {
				sb.append(System.getProperty("line.separator") + getTab(depth)
						+ this.getXMLElementStart(header));
				ArrayList paramList = new ArrayList();
				for (int j = 0; j < paramKeyArray.length; j++) {
					// paramList.add(rstmp.getString((String)
					// paramKeyArray[j]));
					paramList.add((String) valueMap.get(paramKeyArray[j]));
				}
				StringTokenizer st = new StringTokenizer(child, ",");
				while (st.hasMoreTokens()) {
					this.processXML(note + "." + st.nextToken(), depth + 1,
							valueMap, filePath, paramList.toArray());
				}
				sb.append(System.getProperty("line.separator")
						+ this.getTab(depth) + this.getXMLElementEnd(header));
				if (sqlkey != null) {
					this.rowCount++;
					this.writeDatabox(filePath);
				}
				// }
				// daolocal.cleanUp();
			}
		} else {
			if (child == null && value != null) {
				if (!value.startsWith("&&"))
					sb.append(System.getProperty("line.separator")
							+ getTab(depth)
							+ getXMLElementWithValue(header, value));
				else {
					if ((String) valueMap.get(value.replaceAll("&&", "")) != null)
						sb.append(System.getProperty("line.separator")
								+ getTab(depth)
								+ getXMLElementWithValue(header,
										(String) valueMap.get(value.replaceAll(
												"&&", ""))));
				}
			} else if (child != null && value == null) {
				sb.append(System.getProperty("line.separator") + getTab(depth)
						+ this.getXMLElementStart(header));
				/*
				 * ArrayList paramList = new ArrayList(); for (int j = 0; j <
				 * paramKeyArray.length; j++)
				 * paramList.add(rs.getString((String)paramKeyArray[j]));
				 */
				StringTokenizer st = new StringTokenizer(child, ",");
				while (st.hasMoreTokens()) {
					this.processXML(note + "." + st.nextToken(), depth + 1,
							valueMap, filePath, params);
				}
				sb.append(System.getProperty("line.separator")
						+ this.getTab(depth) + this.getXMLElementEnd(header));
			}
		}
		// System.out.println(sb.toString());
		// if (sb.length() >= 1024 * 1024) {
		// long t2 = System.currentTimeMillis();
		// FileOperator.Write(filePath, sb.toString().getBytes(), true);
		// this.iotime += System.currentTimeMillis() - t2;
		// // System.out.println(note + ":iotime=" + this.iotime);
		// // sb.delete(0, -1);
		// sb = new StringBuffer();
		// }
		logger.debug("processXML(note=" + note + ",depth=" + depth
				+ ",valueMap=" + valueMap + ",filepath=" + filePath
				+ ",params=" + Arrays.asList(params) + ") - end.");
	}

	private long iotime;

	private void getDataboxPath(String filePath){
		int page = 0;
		if (rowCount > 0 && rowCount % this.databoxSize == 0)
			page = (int) this.rowCount / this.databoxSize;
		else
			page = (int) this.rowCount / this.databoxSize + 1;
		if (page > 1)
			databoxPath = filePath.substring(0, filePath.lastIndexOf(".")) + "_" + page + ".xml";
		else
			databoxPath = filePath;
	}
	
	private void writeDatabox(String filePath) throws IOException {
//		String databoxPath;
		if (rowCount > 0 && rowCount % this.databoxSize == 0) {
			int page = (int) this.rowCount / this.databoxSize;
			if (page > 1)
				databoxPath = filePath.substring(0, filePath.lastIndexOf(".")) + "_" + page + ".xml";
			else
				databoxPath = filePath;
			long t2 = System.currentTimeMillis();
			File f = new File(databoxPath);
			if (!f.exists()) {
				FileOperator.Write(databoxPath, databoxAppTag.toString()
						.getBytes(), true);
			}
			FileOperator.Write(databoxPath,
					(sb.toString() + this.databoxAppTagEnd).getBytes(), true);
			this.iotime += System.currentTimeMillis() - t2;
			sb = new StringBuffer();
		} else {
			if (this.sb.length() >= 1024 * 1024) {
				int page = (int) this.rowCount / this.databoxSize + 1;
				if (page > 1)
					databoxPath = filePath.substring(0, filePath.lastIndexOf(".")) + "_" + page + ".xml";
				else
					databoxPath = filePath;
				long t2 = System.currentTimeMillis();
				File f = new File(databoxPath);
				if (!f.exists()) {
					FileOperator.Write(databoxPath, databoxAppTag.toString()
							.getBytes(), true);
				}
				FileOperator.Write(databoxPath, sb.toString().getBytes(), true);
				this.iotime += System.currentTimeMillis() - t2;
				sb = new StringBuffer();
			}
		}
	}

	private String translateXMLEscape(String src) throws FileNotFoundException,
			IOException {
		synchronized (mapXMLEscape) {
			//2010.06.01 Jason String ret = new String(src.trim());
			String ret = new String(src);
			Set set = getMapXMLEscape().keySet();
			Iterator it = set.iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				ret = ret.replaceAll(key, (String) mapXMLEscape.get(key));
			}
			return ret;
		}
	}

	private String getHeader(String prefix) throws FileNotFoundException,
			IOException {
		synchronized (propxmlexport) {
			if (prefix == null || "".equals(prefix))
				return getPropxmlexport().getProperty("_header");
			else if (getPropxmlexport().getProperty(
					prefix + "._headerfor" + this.subType) != null)
				return getPropxmlexport().getProperty(
						prefix + "._headerfor" + this.subType);
			else
				return getPropxmlexport().getProperty(prefix + "._header");
		}
	}

	private String getTrailer(String prefix) throws FileNotFoundException,
			IOException {
		synchronized (propxmlexport) {
			if (prefix == null || "".equals(prefix))
				return getPropxmlexport().getProperty("_trailer");
			else if (getPropxmlexport().getProperty(
					prefix + "._trailerfor" + this.subType) != null)
				return getPropxmlexport().getProperty(
						prefix + "._trailerfor" + this.subType);
			else
				return getPropxmlexport().getProperty(prefix + "._trailer");
		}
	}

	private String getChild(String prefix) throws FileNotFoundException,
			IOException {
		synchronized (propxmlexport) {
			if (prefix == null || "".equals(prefix))
				return getPropxmlexport().getProperty("_child");
			else if (getPropxmlexport().getProperty(
					prefix + "._childfor" + this.subType) != null)
				return getPropxmlexport().getProperty(
						prefix + "._childfor" + this.subType);
			else
				return getPropxmlexport().getProperty(prefix + "._child");
		}
	}

	private String getValue(String prefix) throws FileNotFoundException,
			IOException {
		synchronized (propxmlexport) {
			if (prefix == null || "".equals(prefix))
				return getPropxmlexport().getProperty("_value");
			else if (getPropxmlexport().getProperty(
					prefix + "._valuefor" + this.subType) != null)
				return getPropxmlexport().getProperty(
						prefix + "._valuefor" + this.subType);
			else
				return getPropxmlexport().getProperty(prefix + "._value");
		}
	}

	// private String getSql(String prefix) throws FileNotFoundException,
	// IOException {
	// synchronized (propxmlexport) {
	// if (prefix == null || "".equals(prefix))
	// return getPropxmlexport().getProperty("_sql");
	// else
	// return getPropxmlexport().getProperty(prefix + "._sql");
	// }
	// }
	private String getSqlkey(String prefix) throws FileNotFoundException,
			IOException {
		synchronized (propxmlexport) {
			if (prefix == null || "".equals(prefix))
				return getPropxmlexport().getProperty("_sqlkey");
			else if (getPropxmlexport().getProperty(
					prefix + "._sqlkeyfor" + this.subType) != null)
				return getPropxmlexport().getProperty(
						prefix + "._sqlkeyfor" + this.subType);
			else
				return getPropxmlexport().getProperty(prefix + "._sqlkey");
		}
	}

	private String getSqlTble(String prefix) throws FileNotFoundException,
			IOException {
		synchronized (propxmlexport) {
			if (prefix == null || "".equals(prefix))
				return getPropxmlexport().getProperty("_sqltable");
			else if (getPropxmlexport().getProperty(
					prefix + "._sqltablefor" + this.subType) != null)
				return getPropxmlexport().getProperty(
						prefix + "._sqltablefor" + this.subType);
			else
				return getPropxmlexport().getProperty(prefix + "._sqltable");
		}
	}

	private String getSqlFields(String prefix) throws FileNotFoundException,
			IOException {
		synchronized (propxmlexport) {
			if (prefix == null || "".equals(prefix))
				return getPropxmlexport().getProperty("_sqlfields");
			else if (getPropxmlexport().getProperty(
					prefix + "._sqlfieldsfor" + this.subType) != null)
				return getPropxmlexport().getProperty(
						prefix + "._sqlfieldsfor" + this.subType);
			else
				return getPropxmlexport().getProperty(prefix + "._sqlfields");
		}
	}

	private String getSqlWhere(String prefix) throws FileNotFoundException,
			IOException {
		synchronized (propxmlexport) {
			if (prefix == null || "".equals(prefix))
				return getPropxmlexport().getProperty("_sqlwhere");
			else if (getPropxmlexport().getProperty(
					prefix + "._sqlwherefor" + this.subType) != null)
				return getPropxmlexport().getProperty(
						prefix + "._sqlwherefor" + this.subType);
			else
				return getPropxmlexport().getProperty(prefix + "._sqlwhere");
		}
	}

	private String getSqlOrder(String prefix) throws FileNotFoundException,
			IOException {
		synchronized (propxmlexport) {
			if (prefix == null || "".equals(prefix))
				return getPropxmlexport().getProperty("_sqlorder");
			else if (getPropxmlexport().getProperty(
					prefix + "._sqlorderfor" + this.subType) != null)
				return getPropxmlexport().getProperty(
						prefix + "._sqlorderfor" + this.subType);
			else
				return getPropxmlexport().getProperty(prefix + "._sqlorder");
		}
	}

	private String getXMLElementStart(String element)
			throws FileNotFoundException, IOException {
		return "<" + translateXMLEscape(element) + ">";
	}

	private String getXMLElementEnd(String element)
			throws FileNotFoundException, IOException {
		return "</" + translateXMLEscape(element) + ">";
	}

	private String getXMLElementWithValue(String element, String value)
			throws FileNotFoundException, IOException {
		if (value == null)
			return "";
		return "<" + translateXMLEscape(element) + " value=\""
				+ translateXMLEscape(value) + "\"/>";
	}

	private String getTab(int depth) {
		StringBuffer sb1 = new StringBuffer();
		for (int i = 0; i < depth; i++)
			sb1.append("\t");
		return sb1.toString();
	}

	private void initializeDAO() throws SQLException {
		dao = new LogAnalysisDAO(this.configPath);
		// dao1 = new LogAnalysisDAO();
	}

	private void closeDAO() {
		if (dao != null) {
			dao.cleanUp();
			dao.close();
		}
		// if (dao1 != null) {
		// dao1.cleanUp();
		// dao1.close();
		// }
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// check paramter,if param is null, out print 1 and return;
		if (args.length < 2) {
//			logger.error("BatchId can not be null,need input paramters");
			System.out.println("-1");
			return;
		}
		logger = Logger.getLogger(args[0], XMLExport.class);
		try {
			/*
			 * batch batch = export.config.getBatch(args[0]); String basepath =
			 * export.config.getBasePath(); OutputXMLFileList oList =
			 * batch.getOutputXmlFileList(); for (int i=0; i<oList.getFileCount();
			 * i++){ file f = oList.getFile(i); //
			 * logger.info(i+":"+f.getFileName()+","+f.getType()+","+f.getVersionNO());
			 * export.generateDatabox(batch.getId(), basepath + f.getFileName(),
			 * f.getType()); }
			 */
			XMLExport export = new XMLExport(args[0], args[1]);
			export.generateOutputXML();
			System.out.println("0");
		} catch (Exception e) {
			logger.error("an error occured in main().", e);
			System.out.println("1");
		}
	}
}
