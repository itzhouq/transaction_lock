package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.mapper.TestTableMapper;
import org.example.pojo.TransactionReqVO;
import org.example.service.TestTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TestServiceImpl implements TestTableService {

    @Autowired
    private TestTableMapper testTableMapper;

    @Override
    public int update(long userId, long money) {
        return testTableMapper.update(userId, money);
    }

    @Override
    public List<TransactionReqVO> selectAll() {
        List<TransactionReqVO> transactionReqVOList = new ArrayList<>();
        transactionReqVOList.add(TransactionReqVO.builder().id(5L).money(10L).userId(123L).build());
        transactionReqVOList.add(TransactionReqVO.builder().id(6L).money(20L).userId(456L).build());
        transactionReqVOList.add(TransactionReqVO.builder().id(7L).money(30L).userId(789L).build());
        transactionReqVOList.add(TransactionReqVO.builder().id(8L).money(40L).userId(135L).build());
        transactionReqVOList.add(TransactionReqVO.builder().id(9L).money(50L).userId(246L).build());
        return transactionReqVOList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTableData(TransactionReqVO transactionReqVO) {
        try {
            // 删除数据
            testTableMapper.deleteByUserId(transactionReqVO.getUserId());
            // 模拟查询外部接口比较慢
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 循环插入数据
            List<Long> userIdList = new ArrayList<>();
            userIdList.add(123L);
            userIdList.add(456L);
            userIdList.add(789L);
            userIdList.add(135L);
            userIdList.add(246L);

            for (Long userId : userIdList) {
                testTableMapper.insert(111L, userId);
            }
        } catch (Exception e) {
            log.error("异常:",e);
        }
    }
}
