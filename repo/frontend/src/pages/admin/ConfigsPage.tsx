import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, Modal, Table, App as AntdApp } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { apiGet, apiPost } from '../../api/client';

interface SysConfig { id: number; configKey: string; configValue: string; description?: string; }

export default function ConfigsPage() {
  const { message } = AntdApp.useApp();
  const [data, setData] = useState<SysConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<SysConfig>();

  const load = async () => {
    setLoading(true);
    try { setData(await apiGet<SysConfig[]>('/api/admin/configs')); }
    catch (e) { message.error((e as Error).message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const save = async () => {
    try {
      const v = await form.validateFields();
      await apiPost('/api/admin/configs', v);
      message.success('Saved');
      setModalOpen(false);
      load();
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return;
      message.error((e as Error).message);
    }
  };

  const columns: ColumnsType<SysConfig> = [
    { title: 'Key', dataIndex: 'configKey' },
    { title: 'Value', dataIndex: 'configValue' },
    { title: 'Description', dataIndex: 'description', render: (v) => v || '—' },
    {
      title: 'Action',
      render: (_, r) => (
        <Button size="small" onClick={() => { form.setFieldsValue(r); setModalOpen(true); }}>Edit</Button>
      ),
    },
  ];

  return (
    <div className="page-wrapper">
      <Card
        className="page-card"
        title="System configuration"
        extra={<Button type="primary" onClick={() => { form.resetFields(); setModalOpen(true); }}>Add entry</Button>}
      >
        <Table rowKey="id" loading={loading} columns={columns} dataSource={data} />
      </Card>

      <Modal title="System config" open={modalOpen} onOk={save} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="configKey" label="Key" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="configValue" label="Value" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="Description"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
