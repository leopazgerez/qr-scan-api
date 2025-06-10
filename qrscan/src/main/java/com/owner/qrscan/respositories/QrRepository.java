package com.owner.qrscan.respositories;

import com.owner.qrscan.models.Qr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QrRepository  extends JpaRepository<Qr, Long> {
}
