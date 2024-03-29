import { Icon, List, Tag } from 'antd';
import React, { Component } from 'react';
import { connect } from 'dva';
import ArticleListContent from '../ArticleListContent';
import styles from './index.less';

@connect(({ accountAndcenter }) => ({
  list: accountAndcenter.list,
}))
class Articles extends Component {
  render() {
    const { list } = this.props;

    const IconText = ({ type, text }) => (
      <span>
        <Icon
          type={type}
          style={{
            marginRight: 8,
          }}
        />
        {text}
      </span>
    );

    return (
      <List
        size="large"
        className={styles.articleList}
        rowKey="id"
        itemLayout="vertical"
        dataSource={list}
        renderItem={item => (
          <List.Item
            key={item.id}
            actions={[
              <IconText key="star" type="star-o" text={item.star} />,
              <IconText key="like" type="like-o" text={item.like} />,
              <IconText key="message" type="message" text={item.message} />,
            ]}
          >
            <List.Item.Meta
              title={
                <a className={styles.listItemMetaTitle} href={item.href}>
                  {item.title}
                </a>
              }
              description={
                <span>
                  <Tag>Ant Design</Tag>
                  <Tag>设计语言</Tag>
                  <Tag>蚂蚁金服</Tag>
                </span>
              }
            />
            <ArticleListContent data={item} />
          </List.Item>
        )}
      />
    );
  }
}

export default Articles;
