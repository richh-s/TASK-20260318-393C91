import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Space, Table, Tag, App as AntdApp } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { apiGet, apiPost } from '../../api/client';

interface Weight { id: number; factorName: string; weight: number; description?: string; }

export default function WeightsPage() {
  const { message } = AntdApp.useApp();
  const [data, setData] = useState<Weight[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<Weight>();

  const load = async () => {
    setLoading(true);
    try { setData(await apiGet<Weight[]>('/api/admin/weights')); }
    catch (e) { message.error((e as Error).message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const save = async () => {
    try {
      const v = await form.validateFields();
      await apiPost('/api/admin/weights', v);
      message.success('Saved');
      setModalOpen(false);
      load();
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return;
      message.error((e as Error).message);
    }
  };

  const columns: ColumnsType<Weight> = [
    { title: 'Factor', dataIndex: 'factorName' },
    { title: 'Weight', dataIndex: 'weight', render: (v) => <Tag color="purple">{v}</Tag> },
    { title: 'Description', dataIndex: 'description', render: (v) => v || '—' },
  ];

  return (
    <div className="page-wrapper">
      <Card
        className="page-card"
        title="Sorting weights"
        extra={<Button type="primary" onClick={() => { form.resetFields(); setModalOpen(true); }}>Add / update</Button>}
      >
        <Table rowKey="id" loading={loading} columns={columns} dataSource={data} />
      </Card>

      <Modal title="Sorting weight" open={modalOpen} onOk={save} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="factorName" label="Factor name" rules={[{ required: true }]}>
            <Input placeholder="e.g. popularity_score" />
          </Form.Item>
          <Form.Item name="weight" label="Weight (0.0 – 10.0)" rules={[{ required: true }]}>
            <InputNumber min={0} max={10} step={0.1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
