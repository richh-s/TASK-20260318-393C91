import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Table, App as AntdApp } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { apiGet, apiPost, apiDelete } from '../../api/client';

interface FieldDict { id: number; fieldName: string; rawValue: string; standardValue: string; }

export default function DictionariesPage() {
  const { message } = AntdApp.useApp();
  const [data, setData] = useState<FieldDict[]>([]);
  const [loading, setLoading] = useState(false);
  const [filterField, setFilterField] = useState<string>('');
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<FieldDict>();

  const load = async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = {};
      if (filterField) params.fieldName = filterField;
      setData(await apiGet<FieldDict[]>('/api/admin/dictionaries', params));
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [filterField]);

  const save = async () => {
    try {
      const v = await form.validateFields();
      await apiPost('/api/admin/dictionaries', v);
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
      await apiDelete(`/api/admin/dictionaries/${id}`);
      message.success('Deleted');
      load();
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const columns: ColumnsType<FieldDict> = [
    { title: 'Field', dataIndex: 'fieldName' },
    { title: 'Raw value', dataIndex: 'rawValue' },
    { title: 'Standard value', dataIndex: 'standardValue' },
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
        title="Field dictionary"
        extra={
          <Space>
            <Input.Search
              placeholder="Filter by field name"
              allowClear
              style={{ width: 240 }}
              onSearch={(v) => setFilterField(v)}
            />
            <Button type="primary" onClick={() => { form.resetFields(); setModalOpen(true); }}>
              Add entry
            </Button>
          </Space>
        }
      >
        <Table rowKey="id" loading={loading} columns={columns} dataSource={data} />
      </Card>

      <Modal title="Field dictionary entry" open={modalOpen} onOk={save} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="fieldName" label="Field name" rules={[{ required: true }]}>
            <Input placeholder="e.g. stopStatus" />
          </Form.Item>
          <Form.Item name="rawValue" label="Raw value" rules={[{ required: true }]}>
            <Input placeholder="e.g. 运营" />
          </Form.Item>
          <Form.Item name="standardValue" label="Standard value" rules={[{ required: true }]}>
            <Input placeholder="e.g. ACTIVE" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
