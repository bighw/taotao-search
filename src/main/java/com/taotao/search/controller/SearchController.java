package com.taotao.search.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.taotao.search.bean.Item;

@Controller
public class SearchController {

    @Autowired
    private HttpSolrServer httpSolrServer;

    /**
     * 搜索
     * 
     * @param keyWorkds 关键字
     * @param page 当前页
     * @param rows 页面大小
     * @return
     */
    @RequestMapping(value = "search", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> search(@RequestParam("keyWords") String keyWords,
            @RequestParam("page") Integer page, @RequestParam("rows") Integer rows) {

        try {
            SolrQuery solrQuery = new SolrQuery(); // 构造搜索条件
            solrQuery.setQuery("title:" + keyWords + " AND status:1"); // 搜索关键词
            // 设置分页 start=0就是从0开始，，rows=5当前返回5条记录，第二页就是变化start这个值为5就可以了。
            solrQuery.setStart((Math.max(page, 1) - 1) * rows);
            solrQuery.setRows(rows);

            // 是否需要高亮
            boolean isHighlighting = !StringUtils.equals("*", keyWords) && StringUtils.isNotEmpty(keyWords);

            if (isHighlighting) {
                // 设置高亮
                solrQuery.setHighlight(true); // 开启高亮组件
                solrQuery.addHighlightField("title");// 高亮字段
                solrQuery.setHighlightSimplePre("<em>");// 标记，高亮关键字前缀
                solrQuery.setHighlightSimplePost("</em>");// 后缀
            }

            // 执行查询
            QueryResponse queryResponse = this.httpSolrServer.query(solrQuery);

            List<Item> items = queryResponse.getBeans(Item.class);
            if (isHighlighting) {
                // 将高亮的标题数据写回到数据对象中
                Map<String, Map<String, List<String>>> map = queryResponse.getHighlighting();
                for (Map.Entry<String, Map<String, List<String>>> highlighting : map.entrySet()) {
                    for (Item item : items) {
                        if (!highlighting.getKey().equals(item.getId().toString())) {
                            continue;
                        }
                        item.setTitle(StringUtils.join(highlighting.getValue().get("title"), ""));
                        break;
                    }
                }
            }

            // 返回结果集
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("list", items);
            result.put("total", queryResponse.getResults().getNumFound());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

}
