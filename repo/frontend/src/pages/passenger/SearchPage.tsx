import { useEffect, useRef, useState } from 'react';
import { AutoComplete, Button, Card, Empty, Input, List, Space, Tag, Typography, App as AntdApp, Modal, DatePicker, Form, Select } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import { apiGet, apiPost } from '../../api/client';
import type { RouteSearchResult, StopSearchResult } from '../../types';

export default function SearchPage() {
  const { message } = AntdApp.useApp();
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [routes, setRoutes] = useState<RouteSearchResult[]>([]);
  const [stops, setStops] = useState<StopSearchResult[]>([]);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedStop, setSelectedStop] = useState<StopSearchResult | null>(null);
  const [form] = Form.useForm<{ scheduledTime: Dayjs }>();
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!query || !query.trim()) {
      setSuggestions([]);
      return;
    }
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(async () => {
      try {
        const res = await apiGet<string[]>('/api/search/autocomplete', { q: query.trim() });
        setSuggestions(res || []);
      } catch {
        setSuggestions([]);
      }
    }, 250);
  }, [query]);

  const runSearch = async (q: string) => {
    if (!q.trim()) return;
    setLoading(true);
    try {
      const [r, s] = await Promise.all([
        apiGet<RouteSearchResult[]>('/api/search/routes', { q }),
        apiGet<StopSearchResult[]>('/api/search/stops', { q }),
      ]);
      setRoutes(r || []);
      setStops(s || []);
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const openReserve = (stop: StopSearchResult) => {
    setSelectedStop(stop);
    setModalOpen(true);
    form.resetFields();
  };

  const submitReserve = async () => {
    if (!selectedStop) return;
    try {
      const vals = await form.validateFields();
      await apiPost('/api/reservations', {
        routeId: selectedStop.routeId,
        stopId: selectedStop.id,
        scheduledTime: vals.scheduledTime.format('YYYY-MM-DDTHH:mm:ss'),
      });
      message.success('Reservation created');
      setModalOpen(false);
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return;
      message.error((e as Error).message);
    }
  };

  return (
    <div className="page-wrapper">
      <Card className="page-card">
        <div className="section-title">Find a route or stop</div>
        <AutoComplete
          style={{ width: '100%' }}
          options={suggestions.map((s) => ({ value: s }))}
          value={query}
          onChange={setQuery}
          onSelect={(v) => runSearch(v)}
        >
          <Input.Search
            size="large"
            placeholder="Route number, stop name, keywords, or pinyin (e.g. zsl)"
            enterButton="Search"
            loading={loading}
            onSearch={runSearch}
            allowClear
          />
        </AutoComplete>

        <Typography.Title level={5} style={{ marginTop: 24 }}>Routes</Typography.Title>
        {routes.length === 0 ? (
          <Empty description="No routes yet — try a search" />
        ) : (
          <List
            dataSource={routes}
            renderItem={(r) => (
              <List.Item>
                <List.Item.Meta
                  title={<Space><Tag color="blue">{r.routeNumber}</Tag>{r.name}</Space>}
                  description={r.description}
                />
                <Tag>{r.stopCount} stops</Tag>
              </List.Item>
            )}
          />
        )}

        <Typography.Title level={5} style={{ marginTop: 24 }}>Stops</Typography.Title>
        {stops.length === 0 ? (
          <Empty description="No stops yet — try a search" />
        ) : (
          <List
            dataSource={stops}
            renderItem={(s) => (
              <List.Item
                actions={[
                  <Button key="reserve" type="link" onClick={() => openReserve(s)}>
                    Reserve
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  title={
                    <Space>
                      <strong>{s.nameEn}</strong>
                      {s.nameCn && <span style={{ color: '#888' }}>{s.nameCn}</span>}
                    </Space>
                  }
                  description={
                    <Space size="small" wrap>
                      <Tag color="blue">Route {s.routeNumber}</Tag>
                      <span>{s.address || '—'}</span>
                      <Tag>popularity {s.popularityScore}</Tag>
                      <Tag color="purple">score {s.sortScore.toFixed(1)}</Tag>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Card>

      <Modal
        title={selectedStop ? `Reserve at ${selectedStop.nameEn}` : 'Reserve'}
        open={modalOpen}
        onOk={submitReserve}
        onCancel={() => setModalOpen(false)}
        okText="Confirm reservation"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="scheduledTime"
            label="Scheduled arrival time"
            rules={[{ required: true, message: 'Pick a time' }]}
          >
            <DatePicker
              showTime
              style={{ width: '100%' }}
              disabledDate={(d) => !!d && d.isBefore(dayjs().startOf('day'))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
