package com.app.wooridooribe.repository.file;

import com.app.wooridooribe.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FileRepository extends JpaRepository<File, Long> {
    
    @Query("SELECT COALESCE(MAX(f.id), 0) FROM File f")
    Long findMaxId();
}

