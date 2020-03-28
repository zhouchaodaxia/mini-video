package com.mini.video.mapper;

import java.util.List;

import com.mini.video.pojo.Comments;
import com.mini.video.pojo.vo.CommentsVO;
import com.mini.video.utils.MyMapper;

public interface CommentsMapperCustom extends MyMapper<Comments> {
	
	public List<CommentsVO> queryComments(String videoId);
}