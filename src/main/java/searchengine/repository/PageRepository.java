package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface PageRepository extends CrudRepository <Page, Integer> {
    Page findByPath(String path);
    List<Page> findAllBySiteId(Integer siteId);
}
