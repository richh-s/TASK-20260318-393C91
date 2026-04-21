import { useEffect, useState } from 'react';
import { Button, Card, List, Space, Tag, Typography, App as AntdApp, Badge } from 'antd';
import dayjs from 'dayjs';
import { apiGet, apiPatch } from '../../api/client';
import type { NotificationResponse, Page } from '../../types';

const typeColor: Record<string, string> = {
  RESERVATION_SUCCESS: 'green',
  ARRIVAL_REMINDER: 'blue',
  MISSED_CHECKIN: 'red',
  TASK_ASSIGNED: 'gold',
  TASK_ESCALATED: 'volcano',
  SYSTEM: 'default',
};

export default function MessagesPage() {
  const { message } = AntdApp.useApp();
  const [items, setItems] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [unread, setUnread] = useState(0);

  const load = async () => {
    setLoading(true);
    try {
      const res = await apiGet<Page<NotificationResponse>>('/api/notifications', { page: 0, size: 50 });
      setItems(res.content);
      const count = await apiGet<number>('/api/notifications/unread-count');
      setUnread(count);
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const markOne = async (id: number) => {
    await apiPatch(`/api/notifications/${id}/read`);
    load();
  };

  const markAll = async () => {
    await apiPatch('/api/notifications/read-all');
    message.success('All marked as read');
    load();
  };

  return (
    <div className="page-wrapper">
      <Card
        className="page-card"
        title={
          <Space>
            <span>Message center</span>
            <Badge count={unread} />
          </Space>
        }
        extra={
          <Button onClick={markAll} disabled={unread === 0}>
            Mark all read
          </Button>
        }
      >
        <List
          loading={loading}
          dataSource={items}
          locale={{ emptyText: 'No messages yet' }}
          renderItem={(n) => (
            <List.Item
              actions={
                !n.read
                  ? [<Button key="r" size="small" onClick={() => markOne(n.id)}>Mark read</Button>]
                  : [<Tag key="r" color="default">Read</Tag>]
              }
            >
              <List.Item.Meta
                title={
                  <Space>
                    <Tag color={typeColor[n.type] || 'default'}>{n.type.replace(/_/g, ' ')}</Tag>
                    <strong>{n.title}</strong>
                    {!n.read && <Badge status="processing" />}
                  </Space>
                }
                description={
                  <Space direction="vertical" size={2} style={{ width: '100%' }}>
                    <Typography.Text>{n.content}</Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {dayjs(n.createdAt).format('YYYY-MM-DD HH:mm:ss')}
                    </Typography.Text>
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
}
