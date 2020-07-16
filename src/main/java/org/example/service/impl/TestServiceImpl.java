package org.example.service.impl;

import org.example.mapper.TestTableMapper;
import org.example.service.TestTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestTableService {

    @Autowired
    private TestTableMapper testTableMapper;

    @Override
    public int update(long userId, long money) {
        return testTableMapper.update(userId, money);
    }
}
