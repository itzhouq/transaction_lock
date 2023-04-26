package org.example.mapper;

import org.apache.ibatis.annotations.*;


@Mapper
public interface TestTableMapper {

    @Update("UPDATE test_table SET money = money + #{money} WHERE user_id = #{userId}")
    int update(@Param("userId") long userId, @Param("money") long money);

    @Update("UPDATE test_table SET money = money + #{money}")
    int updateMoney(@Param("money") long money);

    @Delete("delete from test_table where user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO `test_table` (`money`, `user_id`) VALUES (#{money}, #{userId});")
    int insert(@Param("money") Long money, @Param("userId") Long userId);
}
