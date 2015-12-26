package com.taotao.search.mq.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.taotao.search.bean.OrderSearch;

@Service
public class OrderSearchService {

    @Autowired
    private HttpSolrServer httpSolrServer;

    @Value("${TAOTAO_ORDER_SOLR}")
    private String TAOTAO_ORDER_SOLR;

    public Map<String, Object> search(String keyWords, Long userId, Integer page, Integer rows) {
        // 切换到order的core
        this.httpSolrServer.setBaseURL(TAOTAO_ORDER_SOLR);

        try {
            SolrQuery solrQuery = new SolrQuery(); // 构造搜索条件
            solrQuery.setQuery("searchField:" + keyWords + " AND userId:" + userId); // 搜索关键词
            // 设置分页 start=0就是从0开始，，rows=5当前返回5条记录，第二页就是变化start这个值为5就可以了。
            solrQuery.setStart((Math.max(page, 1) - 1) * rows);
            solrQuery.setRows(rows);

            // 执行查询
            QueryResponse queryResponse = this.httpSolrServer.query(solrQuery);

            List<OrderSearch> items = queryResponse.getBeans(OrderSearch.class);
            // 返回结果集
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("list", items);
            result.put("total", queryResponse.getResults().getNumFound());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
