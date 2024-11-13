package com.firsttech.insurance.odmchecking.service.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUtil {

	// 讀取檔案
	public static List<String> readLinesFromFile(String filePath){
		List<String> lines = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return lines;
	}
	
	public static Map<String, String> getLocalIpInfo (String infoFilePath) {
		Map<String, String> map = new HashMap<>();
		
		List<String> list = FileUtil.readLinesFromFile(infoFilePath);
		if (list == null || list.size() == 0) {
			System.out.println("[CRON JOB] 沒有找到正確的IP設定檔案，請確認");
			return map;
		}
		
		for (String lineStr : list) {
			if (lineStr == null || !lineStr.contains("=")) {
				continue;
			}
			
			String[] keyVal = lineStr.split("=");
			map.put(keyVal[0], keyVal[1]);
		}
		
		return map;
	}
}