package org.example.manager.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.manager.BizManager;
import org.example.pojo.TransactionReqVO;
import org.example.service.TestTableService;
import org.example.util.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

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

    @Override
    @Transactional(propagation= Propagation.SUPPORTS)
    public void testData() {
        // 从表中查询所有数据
        List<TransactionReqVO> tableList = testTableService.selectAll();
        log.info("数据:{}", tableList);

        // 测试多线程初始化数据
        int threadNum = 4;
        ForkJoinPool executor = new ForkJoinPool(threadNum); // 线程池
        ThreadUtils.multiThreadRun(threadNum, tableList, (teaList) -> batchInitO2OTeacherSalary(teaList), executor);
//        this.batchInitO2OTeacherSalary(tableList);

    }

    // 批量初始化数据
    private void batchInitO2OTeacherSalary(List<TransactionReqVO> teaList) {
        for (TransactionReqVO teaInfo : teaList) {
            testTableService.updateTableData(teaInfo);
        }
    }
}
