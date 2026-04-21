import { useEffect, useState } from 'react';
import { Card, Table, Tag, Space, App as AntdApp } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { Link } from 'react-router-dom';
import { apiGet } from '../../api/client';
import type { Page, WorkflowTaskResponse } from '../../types';

const statusColor = (s: string) =>
  s === 'PENDING' ? 'blue'
    : s === 'APPROVED' ? 'green'
    : s === 'REJECTED' ? 'red'
    : s === 'ESCALATED' ? 'volcano'
    : 'default';

export default function MyTasksPage() {
  const { message } = AntdApp.useApp();
  const [data, setData] = useState<WorkflowTaskResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const res = await apiGet<Page<WorkflowTaskResponse>>('/api/workflow/tasks/my', { page, size });
        setData(res.content);
        setTotal(res.totalElements);
      } catch (e) {
        message.error((e as Error).message);
      } finally {
        setLoading(false);
      }
    })();
  }, [page, size]);

  const columns: ColumnsType<WorkflowTaskResponse> = [
    { title: 'Task #', dataIndex: 'taskNumber' },
    { title: 'Title', render: (_, t) => <Link to={`/dispatcher/tasks/${t.id}`}>{t.title}</Link> },
    { title: 'Type', dataIndex: 'type', render: (v) => <Tag>{v.replace(/_/g, ' ')}</Tag> },
    {
      title: 'Status',
      dataIndex: 'status',
      render: (s, r) => (
        <Space>
          <Tag color={statusColor(s)}>{s}</Tag>
          {r.escalated && <Tag color="volcano">escalated</Tag>}
        </Space>
      ),
    },
    { title: 'Deadline', dataIndex: 'deadline', render: (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—') },
  ];

  return (
    <div className="page-wrapper">
      <Card className="page-card" title="Tasks assigned to me">
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={data}
          pagination={{
            current: page + 1,
            pageSize: size,
            total,
            onChange: (p, s) => { setPage(p - 1); setSize(s); },
          }}
        />
      </Card>
    </div>
  );
}
