package com.firsttech.insurance.odmchecking.service.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
	
	// 寫檔
	public static boolean writeToFile(List<String> contents, String fileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			for (String line : contents) {
				writer.write(line);
				writer.newLine();
			}
			return true;
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return false;
		}
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
	
	public static String formatString(String inputStr, int length, String alignment) {
        if (inputStr == null) {
            inputStr = "";
        }

        // 截斷字符串如果他超過指定長度
        if (inputStr.length() > length) {
            inputStr = inputStr.substring(0, length);
        }

        int paddingSize = length - inputStr.length();
        StringBuilder result = new StringBuilder(length);

        if ("LEFT".equalsIgnoreCase(alignment)) {
            result.append(inputStr);
            for (int i = 0; i < paddingSize; i++) {
                result.append(' ');
            }
        } else if ("CENTER".equalsIgnoreCase(alignment)) {
            int paddingLeft = paddingSize / 2;
            int paddingRight = paddingSize - paddingLeft;
            for (int i = 0; i < paddingLeft; i++) {
                result.append(' ');
            }
            result.append(inputStr);
            for (int i = 0; i < paddingRight; i++) {
                result.append(' ');
            }
        } else if ("RIGHT".equalsIgnoreCase(alignment)) {
            for (int i = 0; i < paddingSize; i++) {
                result.append(' ');
            }
            result.append(inputStr);
        } else {
            throw new IllegalArgumentException("Invalid alignment: " + alignment);
        }

        return result.toString();
    }
}
