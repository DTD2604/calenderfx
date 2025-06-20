package com.example.calender;

import com.vvg.pos.util.StringUtil;
import lombok.Getter;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

public class ClientConfig {
	public static final String PARAM_ROOT_URL = "RootUrl";
    @Getter
    private static final ClientConfig instance;

    public static final String URL_FXML_PMS = "UrlFxmlPms";
	public static final String URL_FXML_POS = "UrlFxmlPos";

	private final Properties properties = new Properties();

	static {
		instance = new ClientConfig();
	}

    public ClientConfig() {
		try {
			String path = ClientConfig.class.getPackage().getName();
			System.out.println("==path==" + path);
			path = "/" + StringUtil.replaceAll(path, ".","/") + "/ClientConfig.txt";

			properties.load(new InputStreamReader(Objects.requireNonNull(ClientConfig.class.getResourceAsStream(path)), StandardCharsets.UTF_8));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getParam(String paramName) {
		return StringUtil.nvl(properties.getProperty(paramName));
	}


}
