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
        SELECT
            id,
            phone_number AS phoneNumber,
            user_name AS userName,
            email,
            password_hash AS passwordHash,
            cover_image_url AS coverImageUrl,
            biography
        FROM users
        WHERE deleted_at IS NULL
          AND LOWER(email) = LOWER(#{email})
        LIMIT 1
        """
    )
    RegisteredUser findByEmail(@Param("email") String email);

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
