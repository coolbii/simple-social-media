package com.example.social.auth.mapper;

import java.time.Instant;

import com.example.social.auth.model.PhoneVerificationRequestRecord;
import com.example.social.auth.model.RegistrationTokenRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthVerificationMapper {

    @Select(
        """
        CALL sp_create_phone_verification_request(
            #{phoneNumber},
            #{provider},
            #{providerRequestId},
            #{status},
            #{expiresAt}
        )
        """
    )
    Long createPhoneVerificationRequest(
        @Param("phoneNumber") String phoneNumber,
        @Param("provider") String provider,
        @Param("providerRequestId") String providerRequestId,
        @Param("status") String status,
        @Param("expiresAt") Instant expiresAt
    );

    @Select("CALL sp_find_latest_phone_verification_request_by_phone(#{phoneNumber})")
    PhoneVerificationRequestRecord findLatestVerificationByPhone(@Param("phoneNumber") String phoneNumber);

    @Select(
        """
        CALL sp_update_phone_verification_status(
            #{verificationRequestId},
            #{status},
            #{attemptCount}
        )
        """
    )
    Boolean updateVerificationStatus(
        @Param("verificationRequestId") long verificationRequestId,
        @Param("status") String status,
        @Param("attemptCount") int attemptCount
    );

    @Select("CALL sp_mark_phone_verification_approved(#{verificationRequestId})")
    Boolean markVerificationApproved(@Param("verificationRequestId") long verificationRequestId);

    @Select(
        """
        CALL sp_insert_registration_token(
            #{phoneNumber},
            #{tokenHash},
            #{verificationRequestId},
            #{expiresAt}
        )
        """
    )
    Long insertRegistrationToken(
        @Param("phoneNumber") String phoneNumber,
        @Param("tokenHash") String tokenHash,
        @Param("verificationRequestId") long verificationRequestId,
        @Param("expiresAt") Instant expiresAt
    );

    @Select("CALL sp_find_registration_token_by_hash(#{tokenHash})")
    RegistrationTokenRecord findRegistrationTokenByHash(@Param("tokenHash") String tokenHash);

    @Select("CALL sp_consume_registration_token(#{registrationTokenId})")
    Boolean consumeRegistrationToken(@Param("registrationTokenId") long registrationTokenId);
}
