package com.chanjet.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UpTransFlagMapper {
    @Update("merge into (select * from b_cups_qrcode_trans_detail_his  where check_flag ='N' and resp_code='00' and fee_flag in('M','Y') ) h  " +
            " using B_CUPS_TRANS_DETAIL c on (c.sys_trace=h.settle_trace_no and c.CHK_FLAG='N' and c.transmit_time=h.transmit_time) " +
            " when matched then  update set h.CHECK_FLAG='Y',h.channel_fee=c.fee")
    public int updTransFlag();
}
