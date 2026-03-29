package com.example.social.auth.mapper;

import com.example.social.auth.model.RegisteredUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("CALL sp_find_user_by_phone(#{phoneNumber})")
    RegisteredUser findByPhone(@Param("phoneNumber") String phoneNumber);

    @Select("CALL sp_find_user_by_id(#{userId})")
    RegisteredUser findById(@Param("userId") long userId);

    @Select(
        """
        CALL sp_register_user(
            #{phoneNumber},
            #{userName},
            #{email},
            #{passwordHash}
        )
        """
    )
    Long registerUser(
        @Param("phoneNumber") String phoneNumber,
        @Param("userName") String userName,
        @Param("email") String email,
        @Param("passwordHash") String passwordHash
    );
}
