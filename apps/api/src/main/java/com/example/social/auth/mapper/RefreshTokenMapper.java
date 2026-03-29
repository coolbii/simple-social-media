package com.example.social.auth.mapper;

import java.time.Instant;

import com.example.social.auth.model.RefreshTokenSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RefreshTokenMapper {

    @Select(
        """
        CALL sp_insert_refresh_token(
            #{userId},
            #{tokenHash},
            #{familyId},
            #{parentTokenId},
            #{expiresAt},
            #{userAgent},
            #{ipAddress}
        )
        """
    )
    Long insertRefreshToken(
        @Param("userId") long userId,
        @Param("tokenHash") String tokenHash,
        @Param("familyId") String familyId,
        @Param("parentTokenId") Long parentTokenId,
        @Param("expiresAt") Instant expiresAt,
        @Param("userAgent") String userAgent,
        @Param("ipAddress") String ipAddress
    );

    @Select("CALL sp_find_refresh_token_by_hash(#{tokenHash})")
    RefreshTokenSession findByHash(@Param("tokenHash") String tokenHash);

    @Select("CALL sp_revoke_refresh_token(#{tokenId}, #{revokeReason})")
    Boolean revokeRefreshToken(
        @Param("tokenId") long tokenId,
        @Param("revokeReason") String revokeReason
    );
}
