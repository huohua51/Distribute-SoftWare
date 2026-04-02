package com.example.user.mapper;

import com.example.user.entity.UserDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("""
            select id, user_id, username, status, created_at, updated_at
            from user_account
            where user_id = #{userId}
            """)
    UserDO findByUserId(@Param("userId") Long userId);

    @Insert("""
            insert into user_account(user_id, username, status, created_at, updated_at)
            values(#{userId}, #{username}, #{status}, now(), now())
            """)
    int insert(UserDO user);
}
