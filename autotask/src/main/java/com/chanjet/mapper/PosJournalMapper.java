package com.chanjet.mapper;

import com.chanjet.entity.InlineDetailInfo;
import com.chanjet.entity.PosDetailInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PosJournalMapper {
    @Select("select id,amount,merch_id,fee,card_type,fee_type,trans_code,user_code,trans_date,trans_time,rrn from b_pos_trans_detail  where fee_flag = 'M' and trans_date=to_char(sysdate,'YYYYMMDD') and rownum<=#{num}")
    List<PosDetailInfo> findJournalPos(@Param("num") Integer state);

    @Select("select id,amount,merch_id,fee,card_type,fee_type,trans_code,user_code,trans_date,trans_time,rrn from b_pos_trans_detail_his  where fee_flag = 'M' and trans_date between to_char(sysdate-7,'YYYYMMDD') and to_char(sysdate-1,'YYYYMMDD') and rownum<=#{num}")
    List<PosDetailInfo> findJournalPosHis(@Param("num") Integer state);

    @Update("update b_pos_trans_detail set FEE_FLAG='Y' where id = #{id}")
    public int updFeeFlag(@Param("id") String id);

    @Update("update b_pos_trans_detail_his set FEE_FLAG='Y' where id = #{id}")
    public int updHisFeeFlag(@Param("id") String id);
}
