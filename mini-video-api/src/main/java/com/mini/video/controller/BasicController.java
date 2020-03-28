package com.mini.video.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import com.mini.video.utils.RedisOperator;

@RestController
public class BasicController {
	
	@Autowired
	public RedisOperator redis;
	
	public static final String USER_REDIS_SESSION = "user-redis-session";
	
	// 文件保存的命名空间
	public static String FILE_SPACE;

	@Value("${file_space}")
	public void setFileSpace(String fileSpace) {
		FILE_SPACE = fileSpace;
	}

	// 文件保存的命名空间
	public static String BGM_SPACE;

	@Value("${bgm_space}")
	public void setBgmSpace(String bgmSpace) {
		BGM_SPACE = bgmSpace;
	}

	// ffmpeg所在目录
	public static String FFMPEG_EXE;

	@Value("${ffmpeg_path}")
	public void setFfmpegExe(String FfmpegPath) {
		FFMPEG_EXE = FfmpegPath;
	}

	// 每页分页的记录数
	public static final Integer PAGE_SIZE = 5;
	
}
