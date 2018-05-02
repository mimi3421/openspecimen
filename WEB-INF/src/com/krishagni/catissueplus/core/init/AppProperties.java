package com.krishagni.catissueplus.core.init;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jndi.JndiTemplate;

public class AppProperties implements FactoryBean<Properties> {
	private static final Log logger = LogFactory.getLog(AppProperties.class);

	private static final String JNDI_PREFIX     = "java:/comp/env/";

	private static final String JNDI_CFG_PREFIX = JNDI_PREFIX + "config/";

	private static final String APP_PROPS       = "application.properties";

	private static final String VERSION_PROP    = "buildinfo.version";

	private static final String BUILD_DATE_PROP = "buildinfo.date";

	private static final String REVISION_PROP   = "buildinfo.commit_revision";

	private static final String DS_JNDI_PROP    = "datasource.jndi";

	private static final String DS_DIALECT_PROP = "datasource.dialect";

	private static final String DB_TYPE_PROP    = "database.type";

	private static final String MYSQL_TYPE      = "mysql";

	private static final String MYSQL_DIALECT   = "org.hibernate.dialect.MySQLDialect";

	private static final String ORA_TYPE        = "oracle";

	private static final String ORA_DIALECT     = "org.hibernate.dialect.Oracle10gDialect";

	private static AppProperties instance;


	private Properties props;

	private AppProperties(String contextName) {
		this.props = getProperties(contextName);
	}

	public Properties getProperties() {
		return props;
	}

	public String getBuildDate() {
		return props.getProperty(BUILD_DATE_PROP);
	}

	public String getBuildVersion() {
		return props.getProperty(VERSION_PROP);
	}

	public String getBuildRevision() {
		return props.getProperty(REVISION_PROP);
	}

	@Override
	public Properties getObject() throws Exception {
		return props;
	}

	@Override
	public Class<?> getObjectType() {
		return Properties.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public static AppProperties getInstance() {
		return instance;
	}

	public static AppProperties getInstance(String contextName) {
		if (instance != null) {
			return instance;
		}

		instance = new AppProperties(contextName);
		return instance;
	}

	private Properties getProperties(String contextName) {
		Properties properties = loadBuildProperties();
		properties.putAll(loadExternalConfig(contextName));

		String ds = properties.getProperty(DS_JNDI_PROP);
		if (StringUtils.isBlank(ds)) {
			logger.fatal("Data source is not specified. Application will be non-functional");
		} else if (!ds.startsWith(JNDI_PREFIX)) {
			logger.info("Data source JNDI name is not fully qualified: " + ds);
			ds = ds.trim();

			int idx = 0;
			while (ds.charAt(idx) == '/') {
				++idx;
			}

			ds = JNDI_PREFIX + ds.substring(idx);
			logger.info("Transformed the data source JNDI name to " + ds);
			properties.put(DS_JNDI_PROP, ds);
		}

		String dbType = properties.getProperty(DB_TYPE_PROP);
		if (MYSQL_TYPE.equalsIgnoreCase(dbType)) {
			logger.info("Recognised as using MySQL data source. Initialising the dialect to " + MYSQL_DIALECT);
			properties.put(DS_DIALECT_PROP, MYSQL_DIALECT);
		} else if (ORA_TYPE.equalsIgnoreCase(dbType)) {
			logger.info("Recognised as using Oracle data source. Initialising the dialect to " + ORA_DIALECT);
			properties.put(DS_DIALECT_PROP, ORA_DIALECT);
		} else {
			logger.fatal("Data source type is not specified. Application will be non-functional");
		}

		return properties;
	}

	private Properties loadBuildProperties() {
		InputStream in = null;

		try {
			in = this.getClass().getClassLoader().getResourceAsStream(APP_PROPS);

			Properties props = new Properties();
			props.load(in);
			return props;
		} catch (Exception e) {
			logger.error("Error loading build properties", e);
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private Properties loadExternalConfig(String contextName) {
		InputStream in = null;

		try {
			String externalConfigPath = new JndiTemplate().lookup(JNDI_CFG_PREFIX + contextName, String.class);
			in = new FileInputStream(externalConfigPath);

			Properties props = new Properties();
			props.load(in);

			props.remove(VERSION_PROP);
			props.remove(BUILD_DATE_PROP);
			props.remove(REVISION_PROP);
			return props;
		} catch (Exception e) {
			logger.error("Error loading external config properties for " + contextName, e);
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
}
