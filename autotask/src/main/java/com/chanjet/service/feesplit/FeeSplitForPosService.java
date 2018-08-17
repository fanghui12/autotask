package com.chanjet.service.feesplit;

import com.chanjet.entity.*;
import com.chanjet.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class FeeSplitForPosService extends BaseFeeSplitting {
    private static final Logger logger = LoggerFactory.getLogger(FeeSplitForPosService.class);
    @Autowired
    private PosJournalMapper posJournalMapper;
    @Autowired
    private MerchMapper merchMapper;
    @Autowired
    private AgentMapper agentMapper;
    @Autowired
    private AppendTransDetailMapper appendTransDetailMapper;


    @Transactional
    public void feeSplitting(Integer rowNum) {

        //取出rowNum条待分润交易流水
        List<PosDetailInfo> list = this.posJournalMapper.findJournalPos(rowNum);

        list.forEach(posDetailInfo -> {
            //从流水中找到商户号
            String merch_id = posDetailInfo.getMerch_id();
            //查找商户名称
            MerchInfo merchInfo = merchMapper.findMerchInfo(merch_id);
            //根据商户号查找对应代理商号
            String agent_id = merchMapper.findMerchAgent(merch_id).getAgent_id();
            //查找全部代理商
            String agent_organization = agentMapper.findAllAgentId(agent_id);
            logger.info("代理商组织关系" + agent_organization);
            String agent[] = agent_organization.split("[,]");
            //查找各代理商费率放到list中
            List<AgentInfo> agtInfoList = new ArrayList<AgentInfo>();
            //List<AgentFeeRate> agtFeeRateList = null;
            for (int i = agent.length - 1; i >= 0; i--) {
                String agt = agent[i];
                AgentInfo agentInfo = agentMapper.findAgentInfo(agt);
                if (i == 0 && agtInfoList.get(agent.length - 2).getAgentFeeRate().getFee_type().equals("1")) {
                    agentInfo.setAgentFeeRate(agentMapper.findAgentFeeS(agt));
                } else {
                    agentInfo.setAgentFeeRate(agentMapper.findAgentFee(agt));
                }
                agentInfo.setRatio(agentInfo.getAgentFeeRate().getC_fee_ratio());
                agtInfoList.add(agentInfo);
            }
            //反转agtInfoList
            Collections.reverse(agtInfoList);
            //初始化分润流水实体
            AppendTransDetail appendTransDetail = new AppendTransDetail(posDetailInfo, merchInfo);
            appendTransDetail.setGroup_code("pos");


            //计算分润fee,cost_amount,ratio,
            logger.info("------------开始计算分润----------");
            for (int i = agtInfoList.size() - 1; i >= 0; i--) {
                //计算成本
                calcAgtCost(agtInfoList.get(i), posDetailInfo);
                if (i == agtInfoList.size() - 1) {
                    calcMerchAgtFee(agtInfoList.get(i), posDetailInfo);
                } else {
                    calcAgtFee(agtInfoList.get(i), agtInfoList.get(i + 1), posDetailInfo);
                }

            }
            logger.info("------------分润计算结束----------");
/*赋值appendTransDetail*/
            setAllAgent(appendTransDetail, agtInfoList);

            //插入分润流水
            appendTransDetailMapper.insertAppendTransDetail(appendTransDetail);
            //更新原流水Fee_flag
            posJournalMapper.updFeeFlag(appendTransDetail.getRef_id());
        });

    }


    public void calcAgtCost(AgentInfo agentInfo, PosDetailInfo posDetailInfo) {
        Float cost_amount = 0f;
        //借记卡
        if (posDetailInfo.getCard_type().equals("0")) {
            //标准类
            if (posDetailInfo.getFee_type().equals("B")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getD_fee_rate() / 100;
                Float max = agentInfo.getAgentFeeRate().getD_fee_max();
                if (max > 0 && cost_amount > max) {
                    cost_amount = max;
                }
                logger.info("代理商:" + agentInfo.getAgent_id() + " 借记卡成本:" + cost_amount + " 最大值:" + max + " 费率D_FEE_RATE:" + agentInfo.getAgentFeeRate().getD_fee_rate());
            }
            //优惠类
            else if (posDetailInfo.getFee_type().equals("Y")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getDy_fee_rate() / 100;
                Float max = agentInfo.getAgentFeeRate().getDy_fee_max();
                if (max > 0 && cost_amount > max) {
                    cost_amount = max;
                }
                logger.info("代理商:" + agentInfo.getAgent_id() + " 借记卡成本:" + cost_amount + " 最大值:" + max + " 费率DY_FEE_RATE:" + agentInfo.getAgentFeeRate().getDy_fee_rate());
            }
            //减免类
            else if (posDetailInfo.getFee_type().equals("M")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getDm_fee_rate() / 100;
                Float max = agentInfo.getAgentFeeRate().getDm_fee_max();
                if (max > 0 && cost_amount > max) {
                    cost_amount = max;
                }
                logger.info("代理商:" + agentInfo.getAgent_id() + " 借记卡成本:" + cost_amount + " 最大值:" + max + " 费率DM_FEE_RATE:" + agentInfo.getAgentFeeRate().getDm_fee_rate());
            }
            //云闪付nfc
            else if (posDetailInfo.getFee_type().equals("YN")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getYd_nfc_fee_rate() / 100;
                logger.info("代理商:" + agentInfo.getAgent_id() + " 借记卡成本:" + cost_amount + " 费率YD_NFC_FEE_RATE:" + agentInfo.getAgentFeeRate().getYd_nfc_fee_rate());
            }
            //云闪付双免
            else if (posDetailInfo.getFee_type().equals("YM")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getYd_free_fee_rate() / 100;
                logger.info("代理商:" + agentInfo.getAgent_id() + " 借记卡成本:" + cost_amount + " 费率YD_FREE_FEE_RATE:" + agentInfo.getAgentFeeRate().getYd_free_fee_rate());
            }

        }
        if (posDetailInfo.getCard_type().equals("1")) {
            //标准类
            if (posDetailInfo.getFee_type().equals("B")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getC_fee_rate() / 100;
                Float max = agentInfo.getAgentFeeRate().getC_fee_max();
                if (max > 0 && cost_amount > max) {
                    cost_amount = max;
                }
                logger.info("代理商:" + agentInfo.getAgent_id() + " 贷记卡成本:" + cost_amount + " 最大值:" + max + " 费率D_FEE_RATE:" + agentInfo.getAgentFeeRate().getC_fee_rate());
            }
            //优惠类
            else if (posDetailInfo.getFee_type().equals("Y")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getCy_fee_rate() / 100;
                Float max = agentInfo.getAgentFeeRate().getCy_fee_max();
                if (max > 0 && cost_amount > max) {
                    cost_amount = max;
                }
                logger.info("代理商:" + agentInfo.getAgent_id() + " 借记卡成本:" + cost_amount + " 最大值:" + max + " 费率DY_FEE_RATE:" + agentInfo.getAgentFeeRate().getCy_fee_rate());
            }
            //减免类
            else if (posDetailInfo.getFee_type().equals("M")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getCm_fee_rate() / 100;
                Float max = agentInfo.getAgentFeeRate().getCm_fee_max();
                if (max > 0 && cost_amount > max) {
                    cost_amount = max;
                }
                logger.info("代理商:" + agentInfo.getAgent_id() + " 借记卡成本:" + cost_amount + " 最大值:" + max + " 费率DM_FEE_RATE:" + agentInfo.getAgentFeeRate().getCm_fee_rate());
            }
            //云闪付nfc
            else if (posDetailInfo.getFee_type().equals("YN")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getYc_nfc_fee_rate() / 100;
                logger.info("代理商:" + agentInfo.getAgent_id() + " 借记卡成本:" + cost_amount + " 费率YD_NFC_FEE_RATE:" + agentInfo.getAgentFeeRate().getYc_nfc_fee_rate());
            }
            //云闪付双免
            else if (posDetailInfo.getFee_type().equals("YM")) {
                //成本
                cost_amount = posDetailInfo.getAmount() * agentInfo.getAgentFeeRate().getYc_free_fee_rate() / 100;
                logger.info("代理商:" + agentInfo.getAgent_id() + " 借记卡成本:" + cost_amount + " 费率YD_FREE_FEE_RATE:" + agentInfo.getAgentFeeRate().getYc_free_fee_rate());
            }

        }

        agentInfo.setCost_amount(cost_amount);
    }

}

