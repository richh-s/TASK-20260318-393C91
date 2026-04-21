import { useEffect, useState } from 'react';
import { Card, Table, Tag, Button, Popconfirm, App as AntdApp } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { apiGet, apiDelete } from '../../api/client';
import type { Page, ReservationResponse } from '../../types';

export default function ReservationsPage() {
  const { message } = AntdApp.useApp();
  const [data, setData] = useState<ReservationResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const load = async () => {
    setLoading(true);
    try {
      const res = await apiGet<Page<ReservationResponse>>('/api/reservations', { page, size });
      setData(res.content);
      setTotal(res.totalElements);
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [page, size]);

  const cancel = async (id: number) => {
    try {
      await apiDelete(`/api/reservations/${id}`);
      message.success('Cancelled');
      load();
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const statusColor = (s: string) =>
    s === 'CONFIRMED' ? 'blue' : s === 'CANCELLED' ? 'default' : s === 'MISSED' ? 'red' : 'green';

  const columns: ColumnsType<ReservationResponse> = [
    { title: 'Route', render: (_, r) => <Tag color="blue">{r.routeNumber}</Tag> },
    { title: 'Stop', render: (_, r) => `${r.stopNameEn}${r.stopNameCn ? ' (' + r.stopNameCn + ')' : ''}` },
    { title: 'Scheduled', render: (_, r) => dayjs(r.scheduledTime).format('YYYY-MM-DD HH:mm') },
    { title: 'Status', render: (_, r) => <Tag color={statusColor(r.status)}>{r.status}</Tag> },
    {
      title: 'Action',
      render: (_, r) =>
        r.status === 'CONFIRMED' ? (
          <Popconfirm title="Cancel this reservation?" onConfirm={() => cancel(r.id)}>
            <Button size="small" danger>Cancel</Button>
          </Popconfirm>
        ) : null,
    },
  ];

  return (
    <div className="page-wrapper">
      <Card className="page-card">
        <div className="section-title">My reservations</div>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={data}
          pagination={{
            current: page + 1,
            pageSize: size,
            total,
            onChange: (p, s) => {
              setPage(p - 1);
              setSize(s);
            },
          }}
        />
      </Card>
    </div>
  );
}
