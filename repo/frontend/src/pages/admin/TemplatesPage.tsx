import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, App as AntdApp, InputNumber } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { apiGet, apiPost, apiDelete } from '../../api/client';

interface Template {
  id: number;
  name: string;
  type: string;
  titleTemplate: string;
  contentTemplate: string;
  sensitivityLevel: number;
}

const TYPE_OPTIONS = [
  'RESERVATION_SUCCESS', 'ARRIVAL_REMINDER', 'MISSED_CHECKIN',
  'TASK_ASSIGNED', 'TASK_ESCALATED', 'SYSTEM',
];

export default function TemplatesPage() {
  const { message } = AntdApp.useApp();
  const [data, setData] = useState<Template[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<Template>();

  const load = async () => {
    setLoading(true);
    try {
      const res = await apiGet<Template[]>('/api/admin/templates');
      setData(res);
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openNew = () => {
    form.resetFields();
    form.setFieldsValue({ sensitivityLevel: 0 });
    setModalOpen(true);
  };

  const save = async () => {
    try {
      const v = await form.validateFields();
      await apiPost('/api/admin/templates', v);
      message.success('Saved');
      setModalOpen(false);
      load();
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return;
      message.error((e as Error).message);
    }
  };

  const remove = async (id: number) => {
    try {
      await apiDelete(`/api/admin/templates/${id}`);
      message.success('Deleted');
      load();
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const columns: ColumnsType<Template> = [
    { title: 'Name', dataIndex: 'name' },
    { title: 'Type', dataIndex: 'type', render: (v) => <Tag>{v.replace(/_/g, ' ')}</Tag> },
    { title: 'Title template', dataIndex: 'titleTemplate' },
    { title: 'Sensitivity', dataIndex: 'sensitivityLevel', render: (v) => <Tag color={v >= 2 ? 'red' : v === 1 ? 'orange' : 'default'}>{v}</Tag> },
    {
      title: 'Action',
      render: (_, r) => (
        <Popconfirm title="Delete?" onConfirm={() => remove(r.id)}>
          <Button danger size="small">Delete</Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div className="page-wrapper">
      <Card
        className="page-card"
        title="Notification templates"
        extra={<Button type="primary" onClick={openNew}>Add template</Button>}
      >
        <Table rowKey="id" loading={loading} columns={columns} dataSource={data} />
      </Card>

      <Modal title="Template" open={modalOpen} onOk={save} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="type" label="Type" rules={[{ required: true }]}>
            <Select options={TYPE_OPTIONS.map((t) => ({ value: t, label: t }))} />
          </Form.Item>
          <Form.Item name="titleTemplate" label="Title template" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="contentTemplate" label="Content template" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="sensitivityLevel" label="Sensitivity (0=none, 1=low, 2=high)">
            <InputNumber min={0} max={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
