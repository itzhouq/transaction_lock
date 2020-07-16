package org.example.manager.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.manager.BizManager;
import org.example.pojo.TransactionReqVO;
import org.example.service.TestTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class BizManagerImpl implements BizManager {

    @Autowired
    private TestTableService testTableService;

    @Override
    @Transactional
    public boolean transactionMoney(List<TransactionReqVO> transactionReqVOList) throws Exception {
        for (TransactionReqVO transactionReqVO : transactionReqVOList) {
            // 模拟业务操作
            Thread.sleep(1000);
            int updateCount = testTableService.update(transactionReqVO.getUserId(), transactionReqVO.getMoney());
            if (updateCount == 0) {
                log.error("转账异常：" + transactionReqVO);
            }
        }
        return true;
    }
}
