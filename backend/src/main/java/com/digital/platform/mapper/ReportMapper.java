package com.digital.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.digital.platform.common.request.ReportQuery;
import com.digital.platform.entity.Report;
import com.digital.platform.vo.ReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    /** 举报列表（带商品和举报人信息），待处理的排在前面 */
    IPage<ReportVO> selectReportPage(IPage<ReportVO> page, @Param("query") ReportQuery query);
}
