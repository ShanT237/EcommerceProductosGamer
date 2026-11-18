package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.MensajeChatEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MensajeChatJpaDataRepository extends JpaRepository<MensajeChatEntity, UUID> {

    Optional<MensajeChatEntity> findByRemitenteIdAndClienteId(UUID remitenteId, UUID clienteId);

    /** Sin cursor: los más nuevos primero. */
    List<MensajeChatEntity> findByConversacionIdOrderByEnviadoEnDescIdDesc(UUID conversacionId, Pageable pageable);

    @Query("select m from MensajeChatEntity m where m.conversacionId = :conversacionId "
            + "and (m.enviadoEn < :fecha or (m.enviadoEn = :fecha and m.id < :id)) "
            + "order by m.enviadoEn desc, m.id desc")
    List<MensajeChatEntity> anterioresA(@Param("conversacionId") UUID conversacionId,
                                        @Param("fecha") LocalDateTime fecha,
                                        @Param("id") UUID id,
                                        Pageable pageable);

    @Query("select m from MensajeChatEntity m where m.conversacionId = :conversacionId "
            + "and (m.enviadoEn > :fecha or (m.enviadoEn = :fecha and m.id > :id)) "
            + "order by m.enviadoEn asc, m.id asc")
    List<MensajeChatEntity> posterioresA(@Param("conversacionId") UUID conversacionId,
                                         @Param("fecha") LocalDateTime fecha,
                                         @Param("id") UUID id,
                                         Pageable pageable);

    @Query("select m.conversacionId, count(m) from MensajeChatEntity m, ConversacionChatEntity c "
            + "where m.conversacionId = c.id and m.leidoEn is null and m.remitenteId <> :usuario "
            + "and (c.compradorId = :usuario or c.vendedorId = :usuario) "
            + "group by m.conversacionId")
    List<Object[]> contarNoLeidos(@Param("usuario") UUID usuario);

    @Modifying
    @Query("update MensajeChatEntity m set m.leidoEn = :cuando "
            + "where m.conversacionId = :c and m.remitenteId <> :lector and m.leidoEn is null")
    int marcarLeidos(@Param("c") UUID conversacionId,
                     @Param("lector") UUID lectorId,
                     @Param("cuando") LocalDateTime cuando);
}
