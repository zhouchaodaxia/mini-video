package com.mini.video.controller;

import com.mini.video.enums.VideoStatusEnum;
import com.mini.video.pojo.Bgm;
import com.mini.video.pojo.Comments;
import com.mini.video.pojo.Videos;
import com.mini.video.service.BgmService;
import com.mini.video.service.VideoService;
import com.mini.video.utils.FfmpegUtils;
import com.mini.video.utils.PagedResult;
import com.mini.video.vo.ResponseVO;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;


@RestController
@Api(value="视频相关业务的接口", tags= {"视频相关业务的controller"})
@RequestMapping("/video")
public class VideoController extends BasicController {
	
	@Autowired
	private BgmService bgmService;
	
	@Autowired
	private VideoService videoService;
	
	@ApiOperation(value="上传视频", notes="上传视频的接口")
	@ApiImplicitParams({
		@ApiImplicitParam(name="userId", value="用户id", required=true, 
				dataType="String", paramType="form"),
		@ApiImplicitParam(name="bgmId", value="背景音乐id", required=false, 
				dataType="String", paramType="form"),
		@ApiImplicitParam(name="videoSeconds", value="背景音乐播放长度", required=true, 
				dataType="String", paramType="form"),
		@ApiImplicitParam(name="videoWidth", value="视频宽度", required=true, 
				dataType="String", paramType="form"),
		@ApiImplicitParam(name="videoHeight", value="视频高度", required=true, 
				dataType="String", paramType="form"),
		@ApiImplicitParam(name="desc", value="视频描述", required=false, 
				dataType="String", paramType="form")
	})
	@PostMapping(value="/upload", headers="content-type=multipart/form-data")
	public ResponseVO upload(String userId,
							 String bgmId, double videoSeconds,
							 int videoWidth, int videoHeight,
							 String desc,
							 @ApiParam(value="短视频", required=true)
				MultipartFile file) throws Exception {
		
		if (StringUtils.isBlank(userId)) {
			return ResponseVO.errorMsg("用户id不能为空...");
		}
		
		// 保存到数据库中的相对路径
		String uploadPathDB = "/" + userId + "/video";
		String coverPathDB = "/" + userId + "/cover";
		
		FileOutputStream fileOutputStream = null;
		FileOutputStream fileOutputStream1 = null;
		InputStream inputStream = null;
		InputStream inputStream1 = null;
		// 文件上传的最终保存路径
		String finalVideoPath = "";
		String finalVideoPath1 = "";
		try {
			if (file != null) {
				
				String fileName = file.getOriginalFilename();
				System.out.println("-----------------fileName:-----------" + fileName);
				// abc.mp4
				String arrayFilenameItem[] =  fileName.split("\\.");
				String fileNamePrefix = "";
				for (int i = 0 ; i < arrayFilenameItem.length-1 ; i ++) {
					fileNamePrefix += arrayFilenameItem[i];
				}
				// fix bug: 解决小程序端OK，PC端不OK的bug，原因：PC端和小程序端对临时视频的命名不同
//				String fileNamePrefix = fileName.split("\\.")[0];
				
				if (StringUtils.isNotBlank(fileName)) {
					
					finalVideoPath = FILE_SPACE + uploadPathDB + "/" + fileName;
					finalVideoPath1 = FILE_SPACE + coverPathDB + "/" + fileName;
					// 设置数据库保存的路径
					uploadPathDB += ("/" + fileName);
					coverPathDB += ("/" + fileNamePrefix + ".jpg");
					
					File outFile = new File(finalVideoPath);
					if (outFile.getParentFile() != null || !outFile.getParentFile().isDirectory()) {
						// 创建父文件夹
						outFile.getParentFile().mkdirs();
					}

					File outFile1 = new File(finalVideoPath1);
					if (outFile1.getParentFile() != null || !outFile1.getParentFile().isDirectory()) {
						// 创建父文件夹
						outFile1.getParentFile().mkdirs();
					}
					
					fileOutputStream = new FileOutputStream(outFile);
					inputStream = file.getInputStream();
					IOUtils.copy(inputStream, fileOutputStream);
				}
				
			} else {
				return ResponseVO.errorMsg("上传出错...");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseVO.errorMsg("上传出错...");
		} finally {
			if (fileOutputStream != null) {
				fileOutputStream.flush();
				fileOutputStream.close();
			}
			if (fileOutputStream1 != null) {
				fileOutputStream1.flush();
				fileOutputStream1.close();
			}
		}

		// 新建音视频处理类
		FfmpegUtils ffmpegUtils = new FfmpegUtils(FFMPEG_EXE);
		
		// 判断bgmId是否为空，如果不为空，
		// 那就查询bgm的信息，并且合并视频，生产新的视频
		if (StringUtils.isNotBlank(bgmId)) {
			Bgm bgm = bgmService.queryBgmById(bgmId);
			String mp3InputPath = BGM_SPACE + bgm.getPath();
			String videoInputPath = finalVideoPath;
			String videoWithNoAudioPath = FILE_SPACE + "/" + userId + "/video" + "/" + UUID.randomUUID().toString() + "_NoAudio.mp4";

			// 去掉音频的视频
			ffmpegUtils.videoWithNoAudio(videoInputPath, videoWithNoAudioPath);

			uploadPathDB = "/" + userId + "/video" + "/" + UUID.randomUUID().toString() + ".mp4";
			finalVideoPath = FILE_SPACE + uploadPathDB;

			// 将无音频的视频与bgm合并生生成新的视频
			ffmpegUtils.convertor(videoWithNoAudioPath, mp3InputPath, videoSeconds, finalVideoPath);
		}
		System.out.println("uploadPathDB=" + uploadPathDB);
		System.out.println("finalVideoPath=" + finalVideoPath);
		
		// 对视频进行截图
		ffmpegUtils.getCover(finalVideoPath, FILE_SPACE + coverPathDB);
		
		// 保存视频信息到数据库
		Videos video = new Videos();
		video.setAudioId(bgmId);
		video.setUserId(userId);
		video.setVideoSeconds((float)videoSeconds);
		video.setVideoHeight(videoHeight);
		video.setVideoWidth(videoWidth);
		video.setVideoDesc(desc);
		video.setVideoPath(uploadPathDB);
		video.setCoverPath(coverPathDB);
		video.setStatus(VideoStatusEnum.SUCCESS.value);
		video.setCreateTime(new Date());
		
		String videoId = videoService.saveVideo(video);
		
		return ResponseVO.ok(videoId);
	}
	
//	@ApiOperation(value="上传封面", notes="上传封面的接口")
//	@ApiImplicitParams({
//		@ApiImplicitParam(name="userId", value="用户id", required=true,
//				dataType="String", paramType="form"),
//		@ApiImplicitParam(name="videoId", value="视频主键id", required=true,
//				dataType="String", paramType="form")
//	})
//	@PostMapping(value="/uploadCover", headers="content-type=multipart/form-data")
//	public ResponseVO uploadCover(String userId,
//				String videoId,
//				@ApiParam(value="视频封面", required=true)
//				MultipartFile file) throws Exception {
//
//		if (StringUtils.isBlank(videoId) || StringUtils.isBlank(userId)) {
//			return ResponseVO.errorMsg("视频主键id和用户id不能为空...");
//		}
//
//		// 保存到数据库中的相对路径
//		String uploadPathDB = "/" + userId + "/cover";
//
//		FileOutputStream fileOutputStream = null;
//		InputStream inputStream = null;
//		// 文件上传的最终保存路径
//		String finalCoverPath = "";
//		try {
//			if (file != null) {
//
//				String fileName = file.getOriginalFilename();
//				if (StringUtils.isNotBlank(fileName)) {
//
//					finalCoverPath = FILE_SPACE + uploadPathDB + "/" + fileName;
//					// 设置数据库保存的路径
//					uploadPathDB += ("/" + fileName);
//
//					File outFile = new File(finalCoverPath);
//					if (outFile.getParentFile() != null || !outFile.getParentFile().isDirectory()) {
//						// 创建父文件夹
//						outFile.getParentFile().mkdirs();
//					}
//
//					fileOutputStream = new FileOutputStream(outFile);
//					inputStream = file.getInputStream();
//					IOUtils.copy(inputStream, fileOutputStream);
//				}
//
//			} else {
//				return ResponseVO.errorMsg("上传出错...");
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			return ResponseVO.errorMsg("上传出错...");
//		} finally {
//			if (fileOutputStream != null) {
//				fileOutputStream.flush();
//				fileOutputStream.close();
//			}
//		}
//
//		videoService.updateVideo(videoId, uploadPathDB);
//
//		return ResponseVO.ok();
//	}
	
	/**
	 * 
	 * @Description: 分页和搜索查询视频列表
	 * isSaveRecord：1 - 需要保存
	 * 				 0 - 不需要保存 ，或者为空的时候
	 */
	@PostMapping(value="/showAll")
	public ResponseVO showAll(@RequestBody Videos video, Integer isSaveRecord,
			Integer page, Integer pageSize) throws Exception {
		
		if (page == null) {
			page = 1;
		}
		
		if (pageSize == null) {
			pageSize = PAGE_SIZE;
		}
		
		PagedResult result = videoService.getAllVideos(video, isSaveRecord, page, pageSize);
		return ResponseVO.ok(result);
	}
	
	/**
	 * @Description: 我关注的人发的视频
	 */
	@PostMapping("/showMyFollow")
	public ResponseVO showMyFollow(String userId, Integer page) throws Exception {
		
		if (StringUtils.isBlank(userId)) {
			return ResponseVO.ok();
		}
		
		if (page == null) {
			page = 1;
		}

		int pageSize = 6;
		
		PagedResult videosList = videoService.queryMyFollowVideos(userId, page, pageSize);
		
		return ResponseVO.ok(videosList);
	}
	
	/**
	 * @Description: 我收藏(点赞)过的视频列表
	 */
	@PostMapping("/showMyLike")
	public ResponseVO showMyLike(String userId, Integer page, Integer pageSize) throws Exception {
		
		if (StringUtils.isBlank(userId)) {
			return ResponseVO.ok();
		}
		
		if (page == null) {
			page = 1;
		}

		if (pageSize == null) {
			pageSize = 6;
		}
		
		PagedResult videosList = videoService.queryMyLikeVideos(userId, page, pageSize);
		
		return ResponseVO.ok(videosList);
	}
	
	@PostMapping(value="/hot")
	public ResponseVO hot() throws Exception {
		return ResponseVO.ok(videoService.getHotwords());
	}
	
	@PostMapping(value="/userLike")
	public ResponseVO userLike(String userId, String videoId, String videoCreaterId) 
			throws Exception {
		videoService.userLikeVideo(userId, videoId, videoCreaterId);
		return ResponseVO.ok();
	}
	
	@PostMapping(value="/userUnLike")
	public ResponseVO userUnLike(String userId, String videoId, String videoCreaterId) throws Exception {
		videoService.userUnLikeVideo(userId, videoId, videoCreaterId);
		return ResponseVO.ok();
	}
	
	@PostMapping("/saveComment")
	public ResponseVO saveComment(@RequestBody Comments comment, 
			String fatherCommentId, String toUserId) throws Exception {
		
		comment.setFatherCommentId(fatherCommentId);
		comment.setToUserId(toUserId);
		
		videoService.saveComment(comment);
		return ResponseVO.ok();
	}
	
	@PostMapping("/getVideoComments")
	public ResponseVO getVideoComments(String videoId, Integer page, Integer pageSize) throws Exception {
		
		if (StringUtils.isBlank(videoId)) {
			return ResponseVO.ok();
		}
		
		// 分页查询视频列表，时间顺序倒序排序
		if (page == null) {
			page = 1;
		}

		if (pageSize == null) {
			pageSize = 10;
		}
		
		PagedResult list = videoService.getAllComments(videoId, page, pageSize);
		
		return ResponseVO.ok(list);
	}
	
}
