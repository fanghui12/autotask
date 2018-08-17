package com.chanjet.service.checkcupsqrcode;

import com.chanjet.mapper.UpCupsFlagMapper;
import com.chanjet.mapper.UpTransFlagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class CheckCupsQrCodeService {

    private static final Logger log = LoggerFactory.getLogger("CheckCupsQrCodeService");
    @Autowired
    private UpCupsFlagMapper upCupsFlagMapper;
    @Autowired
    private UpTransFlagMapper upTransFlagMapper;

    @Transactional
    public void checking() throws Exception{
        Date now = new Date();
        log.info("====================CHECK START : "+ now +"========================");

        Integer Num = upTransFlagMapper.updTransFlag();
        log.info("银联二维码流水表---对账平---处理完成，影响条数：" + Num);

        Integer Num1 = upCupsFlagMapper.updCpusFlag();
        log.info("银联二维码对账文件流水表---对账平---处理完成，影响条数：" + Num1);

        log.info("====================CHECK STOP : "+ now +"========================");
    }
}
