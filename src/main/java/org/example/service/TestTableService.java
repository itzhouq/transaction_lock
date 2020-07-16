package org.example.service;

import org.springframework.stereotype.Service;

@Service
public interface TestTableService {

    int update(long userId, long money);
}
