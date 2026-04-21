import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, List, Space, Switch, TimePicker, Tag, App as AntdApp } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import { apiGet, apiPost } from '../../api/client';
import type { NotificationPreference } from '../../types';

interface FormValues {
  routeId?: number;
  stopId?: number;
  reminderMinutes: number;
  enabled: boolean;
  dndEnabled: boolean;
  dndWindow?: [Dayjs, Dayjs];
}

export default function SettingsPage() {
  const { message } = AntdApp.useApp();
  const [items, setItems] = useState<NotificationPreference[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const dndEnabled = Form.useWatch('dndEnabled', form);

  const load = async () => {
    setLoading(true);
    try {
      const res = await apiGet<NotificationPreference[]>('/api/notifications/preferences');
      setItems(res);
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    form.setFieldsValue({ reminderMinutes: 10, enabled: true, dndEnabled: false });
  }, []);

  const submit = async () => {
    try {
      const v = await form.validateFields();
      setSaving(true);
      const body: Record<string, unknown> = {
        routeId: v.routeId || null,
        stopId: v.stopId || null,
        reminderMinutes: v.reminderMinutes,
        enabled: v.enabled,
        dndEnabled: v.dndEnabled,
      };
      if (v.dndEnabled && v.dndWindow) {
        body.dndStart = v.dndWindow[0].format('HH:mm');
        body.dndEnd = v.dndWindow[1].format('HH:mm');
      }
      await apiPost('/api/notifications/preferences', body);
      message.success('Preferences saved');
      load();
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return;
      message.error((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page-wrapper">
      <Card className="page-card" title="Add or update a preference">
        <Form form={form} layout="vertical" onFinish={submit}>
          <Space size="large" wrap>
            <Form.Item name="routeId" label="Route ID (optional)">
              <InputNumber placeholder="leave blank for all" style={{ width: 180 }} />
            </Form.Item>
            <Form.Item name="stopId" label="Stop ID (optional)">
              <InputNumber placeholder="leave blank for all" style={{ width: 180 }} />
            </Form.Item>
            <Form.Item
              name="reminderMinutes"
              label="Reminder minutes"
              rules={[{ required: true, message: 'Required' }]}
            >
              <InputNumber min={1} max={60} style={{ width: 120 }} />
            </Form.Item>
            <Form.Item name="enabled" label="Enabled" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="dndEnabled" label="DND" valuePropName="checked">
              <Switch />
            </Form.Item>
            {dndEnabled && (
              <Form.Item
                name="dndWindow"
                label="DND window (HH:mm)"
                rules={[{ required: true, message: 'Pick a window' }]}
              >
                <TimePicker.RangePicker format="HH:mm" />
              </Form.Item>
            )}
          </Space>
          <Button type="primary" htmlType="submit" loading={saving}>
            Save preference
          </Button>
        </Form>
      </Card>

      <Card className="page-card" title="Current preferences" style={{ marginTop: 16 }}>
        <List
          loading={loading}
          dataSource={items}
          locale={{ emptyText: 'No preferences yet — a default 10-minute reminder will apply' }}
          renderItem={(p) => (
            <List.Item>
              <List.Item.Meta
                title={
                  <Space wrap>
                    <Tag color={p.enabled ? 'green' : 'default'}>{p.enabled ? 'enabled' : 'disabled'}</Tag>
                    <span>
                      Route {p.routeId ?? 'any'} / Stop {p.stopId ?? 'any'} — {p.reminderMinutes} min before
                    </span>
                    {p.dndEnabled && p.dndStart && p.dndEnd && (
                      <Tag color="purple">DND {p.dndStart}–{p.dndEnd}</Tag>
                    )}
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
