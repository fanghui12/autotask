package com.chanjet.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UpCupsFlagMapper {
    @Update("merge into (SELECT * from B_CUPS_TRANS_DETAIL where chk_flag='N' and substr(input_mode,1,2) in('03','93','04','94')) c " +
            " using (select * from b_cups_qrcode_trans_detail_his   where check_flag ='Y' and resp_code='00' and fee_flag in ('M','Y')) h " +
            " on (c.sys_trace=h.settle_trace_no and c.transmit_time=h.transmit_time) " +
            " when matched then  update set c.CHK_FLAG='Y' where  c.chk_flag='N'")
    public int updCpusFlag();


    //abcdefg
}
