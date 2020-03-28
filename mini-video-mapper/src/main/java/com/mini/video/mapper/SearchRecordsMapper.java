package com.mini.video.mapper;

import java.util.List;

import com.mini.video.pojo.SearchRecords;
import com.mini.video.utils.MyMapper;

public interface SearchRecordsMapper extends MyMapper<SearchRecords> {
	
	public List<String> getHotwords();
}